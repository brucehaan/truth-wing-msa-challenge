package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.LedgerPort;
import com.example.demo.settlement.domain.ledger.AccountCode;
import com.example.demo.settlement.domain.ledger.AccountKind;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.JournalHeader;
import com.example.demo.settlement.domain.ledger.JournalType;
import com.example.demo.settlement.domain.ledger.Posting;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * 원장 영속성 (README Step 8). 원장은 JPA 가 아니라 JdbcTemplate 으로 쓴다 — append-only 라 변경 감지·merge 가 필요 없고,
 * 중복은 ON CONFLICT DO NOTHING 으로 알린다(예외로 알리면 PostgreSQL 트랜잭션이 abort 된다).
 * 이 클래스에는 UPDATE/DELETE 문이 없다. DB 차원의 보호는 docs/sql/settlement-ledger-guard.sql.
 */
@Repository
@RequiredArgsConstructor
public class JdbcLedgerAdapter implements LedgerPort {

    private final JdbcTemplate jdbc;

    @Override
    public boolean appendIfAbsent(JournalEntry entry) {
        JournalHeader h = entry.header();
        int inserted = jdbc.update("""
                INSERT INTO settlement.journal_entry
                    (id, type, source_key, order_no, seller_id, business_date, occurred_at,
                     fee_policy_id, reversal_of, issued_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_key) DO NOTHING
                """,
                entry.id(), h.type().name(), h.sourceKey().value(), h.orderNo(), h.sellerId(),
                h.businessDate(), Timestamp.from(h.occurredAt()), h.feePolicyId(), h.reversalOf(), h.issuedBy());

        if (inserted == 0) {
            return false;                       // 이미 있음 — 트랜잭션은 멀쩡하다
        }
        jdbc.batchUpdate("""
                INSERT INTO settlement.journal_posting
                    (id, journal_entry_id, account_kind, account_owner, amount, business_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                entry.postings(), 100, (ps, p) -> {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, entry.id());
                    ps.setString(3, p.account().kind().name());
                    ps.setString(4, p.account().ownerId());
                    ps.setLong(5, p.amount().amount());
                    ps.setObject(6, h.businessDate());
                });
        return true;
    }

    @Override
    public boolean exists(SourceKey key) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM settlement.journal_entry WHERE source_key = ?)",
                Boolean.class, key.value()));
    }

    @Override
    public Money sumPostings(AccountCode account, LocalDate businessDate) {
        Long sum = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount), 0) FROM settlement.journal_posting
                WHERE account_kind = ?
                  AND account_owner IS NOT DISTINCT FROM CAST(? AS VARCHAR)
                  AND business_date = ?
                """, Long.class, account.kind().name(), account.ownerId(), businessDate);
        return Money.won(sum == null ? 0L : sum);
    }

    @Override
    public Money trialBalance(LocalDate businessDate) {
        Long sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM settlement.journal_posting WHERE business_date = ?",
                Long.class, businessDate);
        return Money.won(sum == null ? 0L : sum);
    }

    @Override
    public Set<String> sellerWithPostingsOn(LocalDate businessDate) {
        return new TreeSet<>(jdbc.queryForList("""
                SELECT DISTINCT account_owner FROM settlement.journal_posting
                WHERE business_date = ? AND account_kind = 'SELLER_PAYABLE'
                """, String.class, businessDate));
    }

    @Override
    public Optional<JournalEntry> findBySourceKey(SourceKey key) {
        return load("e.source_key = ?", key.value()).stream().findFirst();
    }

    @Override
    public List<JournalEntry> findByOrderNo(String orderNo) {
        return load("e.order_no = ?", orderNo);
    }

    /* 헤더와 분개를 한 번에 읽어 JournalEntry.restore() 로 되돌린다. restore 가 차대 균형을 다시 검증한다. */
    private List<JournalEntry> load(String where, Object arg) {
        Map<UUID, JournalHeader> headers = new LinkedHashMap<>();
        Map<UUID, List<Posting>> postings = new HashMap<>();
        jdbc.query("""
                SELECT e.id, e.type, e.source_key, e.order_no, e.seller_id, e.business_date, e.occurred_at,
                       e.fee_policy_id, e.reversal_of, e.issued_by,
                       p.account_kind, p.account_owner, p.amount
                FROM settlement.journal_entry e
                JOIN settlement.journal_posting p ON p.journal_entry_id = e.id
                WHERE %s
                ORDER BY e.created_at, e.id
                """.formatted(where), (RowCallbackHandler) rs -> {
                    UUID id = rs.getObject("id", UUID.class);
                    if (!headers.containsKey(id)) {
                        headers.put(id, header(rs));
                    }
                    postings.computeIfAbsent(id, k -> new ArrayList<>()).add(new Posting(
                            new AccountCode(AccountKind.valueOf(rs.getString("account_kind")), rs.getString("account_owner")),
                            Money.won(rs.getLong("amount"))));
                }, arg);
        return headers.entrySet().stream()
                .map(e -> JournalEntry.restore(e.getKey(), e.getValue(), postings.get(e.getKey())))
                .toList();
    }

    private static JournalHeader header(ResultSet rs) throws SQLException {
        return new JournalHeader(
                JournalType.valueOf(rs.getString("type")),
                new SourceKey(rs.getString("source_key")),
                rs.getString("order_no"),
                rs.getString("seller_id"),
                rs.getObject("business_date", LocalDate.class),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getObject("fee_policy_id", UUID.class),
                rs.getObject("reversal_of", UUID.class),
                rs.getString("issued_by"));
    }
}

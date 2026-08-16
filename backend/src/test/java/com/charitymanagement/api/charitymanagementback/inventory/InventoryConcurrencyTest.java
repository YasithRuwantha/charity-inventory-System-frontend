package com.charitymanagement.api.charitymanagementback.inventory;

import com.charitymanagement.api.charitymanagementback.auth.entity.User;
import com.charitymanagement.api.charitymanagementback.common.exception.ApiException;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryCategory;
import com.charitymanagement.api.charitymanagementback.inventory.entity.InventoryItem;
import com.charitymanagement.api.charitymanagementback.inventory.enums.ReferenceType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.TransactionType;
import com.charitymanagement.api.charitymanagementback.inventory.enums.UnitOfMeasure;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryCategoryRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryItemRepository;
import com.charitymanagement.api.charitymanagementback.inventory.repository.InventoryTransactionRepository;
import com.charitymanagement.api.charitymanagementback.inventory.service.InventoryStockService;
import com.charitymanagement.api.charitymanagementback.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent deduction safety. The service is called directly rather than through MockMvc because
 * each thread needs its own transaction against the same row — which is exactly the race the row
 * lock has to win.
 */
class InventoryConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryStockService stockService;

    @Autowired
    private InventoryItemRepository itemRepository;

    @Autowired
    private InventoryCategoryRepository categoryRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    /**
     * The stock methods are {@code Propagation.MANDATORY} — they refuse to run outside a
     * transaction. Each worker therefore opens its own, which is what makes the threads contend for
     * the same row rather than sharing one transaction.
     */
    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Two 8-unit deductions against 10 units: exactly one succeeds and stock never goes negative")
    void competingDeductionsCannotOverdraw() throws Exception {
        long itemId = persistItem("Rice", 10);
        User actor = userRepository.findByEmail(STAFF_EMAIL).orElseThrow();

        List<Boolean> outcomes = runConcurrently(2, () -> deduct(itemId, 8, actor));

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(itemRepository.findById(itemId).orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Twenty parallel single-unit deductions against 10 units settle at exactly zero")
    void parallelDeductionsSettleAtZero() throws Exception {
        long itemId = persistItem("Milk", 10);
        User actor = userRepository.findByEmail(STAFF_EMAIL).orElseThrow();

        List<Boolean> outcomes = runConcurrently(20, () -> deduct(itemId, 1, actor));

        long succeeded = outcomes.stream().filter(Boolean::booleanValue).count();
        assertThat(succeeded).isEqualTo(10);
        assertThat(itemRepository.findById(itemId).orElseThrow().getQuantity()).isZero();
        // One ledger row per successful movement — no phantom or lost entries.
        assertThat(transactionRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("Parallel additions all land: no increase is lost to a race")
    void parallelAdditionsAreNotLost() throws Exception {
        long itemId = persistItem("Soap", 0);
        User actor = userRepository.findByEmail(STAFF_EMAIL).orElseThrow();

        List<Boolean> outcomes = runConcurrently(15, () -> transactionTemplate.execute(tx -> {
            stockService.increase(itemId, 5, TransactionType.DONATION_IN, ReferenceType.DONATION,
                    null, "Concurrent donation", actor, null);
            return true;
        }));

        assertThat(outcomes).allMatch(Boolean::booleanValue);
        assertThat(itemRepository.findById(itemId).orElseThrow().getQuantity()).isEqualTo(75);
    }

    /** One deduction attempt in its own transaction; {@code false} means it was correctly refused. */
    private boolean deduct(long itemId, int quantity, User actor) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(tx -> {
                stockService.decrease(itemId, quantity, TransactionType.DISTRIBUTION_OUT,
                        ReferenceType.DISTRIBUTION, null, "Concurrent hand-over", actor, false);
                return true;
            }));
        } catch (ApiException ex) {
            return false;
        }
    }

    /**
     * Fires {@code threads} copies of the task at the same instant via a start latch, so they
     * genuinely contend rather than running one after another.
     */
    private List<Boolean> runConcurrently(int threads, Callable<Boolean> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger unexpected = new AtomicInteger();
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return task.call();
                    } catch (ApiException ex) {
                        return false;
                    } catch (Exception ex) {
                        // A lock timeout or serialisation failure is still a refusal, not a
                        // successful deduction — but count it so a flood of them is visible.
                        unexpected.incrementAndGet();
                        return false;
                    }
                }));
            }
            start.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
            //noinspection ResultOfMethodCallIgnored
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private long persistItem(String name, int quantity) {
        InventoryCategory category = categoryRepository.save(InventoryCategory.builder()
                .name(name + " category")
                .build());
        return itemRepository.save(InventoryItem.builder()
                .itemCode("INV-TEST-" + name)
                .itemName(name)
                .category(category)
                .quantity(quantity)
                .unit(UnitOfMeasure.KG)
                .minimumStockLevel(0)
                .build()).getId();
    }
}

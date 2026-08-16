package com.charitymanagement.api.charitymanagementback.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Caps client-supplied paging so a single request cannot pull the whole table into memory. */
public final class PageableUtils {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private PageableUtils() {
    }

    /**
     * Rebuilds a {@link Pageable} with a safe size and a sort restricted to a known allow-list of
     * property names, so {@code ?sort=} can never reference an arbitrary entity path.
     */
    public static Pageable sanitize(Pageable pageable, Set<String> sortableProperties, String defaultSort) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE
                : Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        Sort sort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            if (sortableProperties.contains(order.getProperty())) {
                sort = sort.and(Sort.by(order.getDirection(), order.getProperty()));
            }
        }
        if (sort.isUnsorted() && defaultSort != null) {
            sort = Sort.by(Sort.Direction.DESC, defaultSort);
        }
        return PageRequest.of(page, size, sort);
    }
}

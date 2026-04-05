package com.financedashboard.service;

import com.financedashboard.dto.record.FinancialRecordFilterRequest;
import com.financedashboard.dto.record.FinancialRecordRequest;
import com.financedashboard.dto.record.FinancialRecordResponse;
import com.financedashboard.entity.FinancialRecord;
import com.financedashboard.entity.User;
import com.financedashboard.exception.ResourceNotFoundException;
import com.financedashboard.repository.FinancialRecordRepository;
import com.financedashboard.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final FinancialRecordRepository financialRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public FinancialRecordResponse createRecord(FinancialRecordRequest request, String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.amount())
                .type(request.type())
                .category(request.category().trim())
                .date(request.date())
                .description(request.description().trim())
                .user(user)
                .build();

        FinancialRecord savedRecord = financialRecordRepository.save(record);
        log.info("Created financial record {} for user {}", savedRecord.getId(), user.getEmail());
        return FinancialRecordResponse.from(savedRecord);
    }

    @Transactional(readOnly = true)
    public Page<FinancialRecordResponse> getAllRecords(FinancialRecordFilterRequest filter) {
        validateFilter(filter);

        Pageable pageable = PageRequest.of(
                resolvePage(filter.page()),
                resolveSize(filter.size()),
                Sort.by(resolveDirection(filter.sortDirection()), resolveSortProperty(filter.sortBy())));

        Specification<FinancialRecord> specification = buildSpecification(filter);
        return financialRecordRepository.findAll(specification, pageable).map(FinancialRecordResponse::from);
    }

    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(Long id) {
        return FinancialRecordResponse.from(findRecord(id));
    }

    @Transactional
    public FinancialRecordResponse updateRecord(Long id, FinancialRecordRequest request) {
        FinancialRecord record = findRecord(id);
        record.setAmount(request.amount());
        record.setType(request.type());
        record.setCategory(request.category().trim());
        record.setDate(request.date());
        record.setDescription(request.description().trim());

        FinancialRecord savedRecord = financialRecordRepository.save(record);
        log.info("Updated financial record {}", savedRecord.getId());
        return FinancialRecordResponse.from(savedRecord);
    }

    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = findRecord(id);
        financialRecordRepository.delete(record);
        log.info("Soft deleted financial record {}", id);
    }

    private Specification<FinancialRecord> buildSpecification(FinancialRecordFilterRequest filter) {
        Specification<FinancialRecord> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (filter.fromDate() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("date"), filter.fromDate()));
        }
        if (filter.toDate() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("date"), filter.toDate()));
        }
        if (filter.type() != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("type"), filter.type()));
        }
        if (StringUtils.hasText(filter.category())) {
            String category = filter.category().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), "%" + category + "%"));
        }
        if (StringUtils.hasText(filter.search())) {
            String search = filter.search().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + search + "%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), "%" + search + "%")));
        }

        return specification;
    }

    private FinancialRecord findRecord(Long id) {
        return financialRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record not found with id: " + id));
    }

    private void validateFilter(FinancialRecordFilterRequest filter) {
        if (filter.fromDate() != null && filter.toDate() != null && filter.fromDate().isAfter(filter.toDate())) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private int resolvePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String resolveSortProperty(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "date";
        }
        return switch (sortBy.trim()) {
            case "amount", "category", "date", "type", "description" -> sortBy.trim();
            default -> "date";
        };
    }

    private Sort.Direction resolveDirection(String sortDirection) {
        if (!StringUtils.hasText(sortDirection)) {
            return Sort.Direction.DESC;
        }
        try {
            return Sort.Direction.fromString(sortDirection.trim());
        } catch (IllegalArgumentException exception) {
            return Sort.Direction.DESC;
        }
    }
}

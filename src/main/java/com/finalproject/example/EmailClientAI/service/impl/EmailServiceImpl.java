package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.ListEmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.PubSubMessageDTO;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.enumeration.EmailLabel;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.mapper.EmailMapper;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.security.SecurityUtils;
import com.finalproject.example.EmailClientAI.service.EmailService;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final EmailRepository emailRepository;
    private final EmailMapper emailMapper;

    @Override
    public ListEmailDTO listEmails(Map<String, String> filters, Pageable pageable) {
        var result = new ListEmailDTO();

        Specification<Email> query = (root, q, cb) -> {
            return cb.conjunction();
        };

        var currLoggedInUser = SecurityUtils.getCurrentLoggedInUser().orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_FOUND)
        );
        query = query.and((root, q, cb) ->
                cb.equal(root.get("userId"), currLoggedInUser.getId())
        );

        query = applyFilters(filters, query);

        var emails = emailRepository.findAll(query, pageable);
        result.setTotal(emails.getTotalElements());
        result.setTotalPages(emails.getTotalPages());
        result.setCurrentPage(pageable.getPageNumber());
        result.setEmails(emails.stream()
                .map(email -> {
                    var emailDTO =  emailMapper.toDto(email);
                    // Remove body content for listing to reduce payload size
                    emailDTO.setBodyHtml(null);
                    emailDTO.setBodyText(null);
                    return emailDTO;
                })
                .toList());

        return result;
    }

    @Override
    public EmailDTO getDetails(UUID id) {
        var currLoggedInUser = SecurityUtils.getCurrentLoggedInUser().orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_FOUND)
        );
        var email = emailRepository.findByIdAndUserId(id, currLoggedInUser.getId()).orElseThrow(
                () -> new AppException(ErrorCode.EMAIL_NOT_FOUND)
        );
        return emailMapper.toDto(email);
    }


    private Specification<Email> applyFilters(Map<String, String> filters, Specification<Email> query) {
        var searchString = filters.remove("s");
        var from = Optional.ofNullable(filters.remove("from")).filter(StringUtils::isNotBlank).map(Instant::parse).orElse(null);
        var to = Optional.ofNullable(filters.remove("to")).filter(StringUtils::isNotBlank).map(Instant::parse).orElse(null);
        var category = filters.remove("category");


        if (StringUtils.isNotBlank(searchString)) {
            query = query.and(applySearchFilter(searchString));
        }

        if (Objects.nonNull(from)) {
            query = query.and(applyFromFilter(from));
        }

        if (Objects.nonNull(category)) {
            query = query.and(applyCategoryFilter(category));
        }



        if (Objects.nonNull(to)) {
            query = query.and(applyToFilter(to));
        }

//        filters.keySet().removeAll(Set.of("page", "size", "sort", "recordType"));
//
//        for (Map.Entry<String, String> entry : filters.entrySet()) {
//            query = query.and(applyCustomFilter(entry.getKey(), entry.getValue()));
//        }
        return query;
    }

    private Specification<Email> applySearchFilter(String searchString) {
        return null;
    }

    private Specification<Email> applyFromFilter(Instant from) {
        return null;
    }

    private Specification<Email> applyToFilter(Instant to) {
        return null;
    }

    private Specification<Email> applyCategoryFilter(String categoryString) {
        var category = EmailLabel.fromId(categoryString);
        if (category == null) {
            throw new AppException(ErrorCode.INVALID_EMAIL_CATEGORY);
        }
        return (root, q, cb) -> {
            var labelsJoin = root.joinSet("labels", JoinType.LEFT);
            return cb.equal(labelsJoin, category.toString());
        };
    }
}

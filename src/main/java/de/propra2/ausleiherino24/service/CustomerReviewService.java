package de.propra2.ausleiherino24.service;

import de.propra2.ausleiherino24.data.CustomerReviewRepository;
import de.propra2.ausleiherino24.model.CustomerReview;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerReviewService {

    private final CustomerReviewRepository customerReviewRepository;
    private final CaseService caseService;

    @Autowired
    public CustomerReviewService(final CustomerReviewRepository customerReviewRepository,
            final CaseService caseService) {
        this.customerReviewRepository = customerReviewRepository;
        this.caseService = caseService;
    }

    /**
     * Saves the given customerReview in database.
     **/
    public void saveReview(final CustomerReview customerReview) {
        customerReviewRepository.save(customerReview);
    }

    /**
     * Returns list of all Reviews, in which cases the given person is the article owner.
     *
     * @param id personId
     */
    List<CustomerReview> findAllReviewsByLenderId(final Long id) {
        final List<CustomerReview> reviews = customerReviewRepository.findAll();
        return reviews.stream()
                .filter(customerReview -> caseService.getAllCasesFromPersonOwner(id)
                        .contains(customerReview.getAcase()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all customer reviews.
     */
    public List<CustomerReview> findAllReviews() {
        return customerReviewRepository.findAll();
    }
}

package de.propra2.ausleiherino24.web;

import de.propra2.ausleiherino24.features.category.Category;
import de.propra2.ausleiherino24.model.Case;
import de.propra2.ausleiherino24.model.CustomerReview;
import de.propra2.ausleiherino24.service.CaseService;
import de.propra2.ausleiherino24.service.CustomerReviewService;
import java.beans.PropertyEditorSupport;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 'CaseController' handles all requests Case-related. This includes booking of articles (and
 * therefore case creation), accepting/declining requests, returning articles (case closing) and
 * customer reviews.
 */
@Controller
public class CaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseController.class);

    private final CaseService caseService;
    private final CustomerReviewService customerReviewService;

    @Autowired
    public CaseController(final CaseService caseService,
            final CustomerReviewService customerReviewService) {
        this.caseService = caseService;
        this.customerReviewService = customerReviewService;
    }

    /**
     * Method is needed, so that the calender shows, on which days the article is already reserved.
     */
    @GetMapping("/api/events")
    @ResponseBody
    public List<LocalDate> test() {
        return caseService.findAllReservedDaysByArticle((long) 3);
    }

    /**
     * Books an article. Calls caseService.requestArticle
     *
     * @return redirect: /article?id
     */
    @PostMapping("/bookArticle")
    public String bookArticle(final @RequestParam Long id, final String startDate,
            final String endDate, final Principal principal) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        final String url = "redirect:/article?id=" + id;

        try {
            if (caseService.requestArticle(
                    id,
                    simpleDateFormat.parse(startDate).getTime(),
                    simpleDateFormat.parse(endDate).getTime(),
                    principal.getName())) {
                return url + "&success";
            } else {
                return url + "&failed";
            }
        } catch (ParseException e) {
            LOGGER.error("Could not book article {}.", id, e);
        }

        return url;
    }


    /**
     * Principal buys an article.
     *
     * @param articleId article that is sold
     * @param principal customer that buys article
     */
    @PostMapping("/buyArticle")
    public String buyArticle(final @RequestParam Long articleId,
            final Principal principal) {
        if (caseService.sellArticle(articleId, principal)) {
            return "redirect:/?access";
        }
        return "redirect:/?failed";
    }

    /**
     * Mapping for user to accept an request.
     *
     * @param id caseId
     * @return Redirects to myOverview with fitting warning.
     */
    @PostMapping("/acceptCase")
    public String acceptCase(final @RequestParam Long id) {
        switch (caseService.acceptArticleRequest(id)) {
            case 1:
                return "redirect:/myOverview?requests";
            case 2:
                return "redirect:/myOverview?requests&alreadyRented";
            case 3:
                return "redirect:/myOverview?requests&receiverOutOfMoney";
            case 4:
                return "redirect:/myOverview?requests&propayUnavailable";
            default:
                return "redirect:/myOverview?requests&error";
        }
    }

    /**
     * declines a Case.
     */
    @PostMapping("/declineCase")
    public String declineCase(final @RequestParam Long id) {
        if (caseService.declineArticleRequest(id)) {
            return "redirect:/myOverview?requests&declined";
        }
        return "redirect:/myOverview?request&propayUnavailable";
    }

    /**
     * accepts the return of a Case.
     */
    @PostMapping("/acceptCaseReturn")
    public String acceptCaseReturn(final @RequestParam Long id) {
        if (caseService.acceptCaseReturn(id)) {
            return "redirect:/myOverview?returned&successfullyReturned";
        }
        return "redirect:/myOverview?returned&propayUnavailable";
    }

    /**
     * Mapping for creating an review.
     *
     * @param id caseId
     * @param review object
     * @return redirect: /myOverview with fitting param
     */
    @PostMapping("/writeReview")
    public String writeReview(final @RequestParam Long id, final CustomerReview review) {
        final Case opt = caseService.findCaseById(id);

        review.setTimestamp(new Date().getTime());
        review.setAcase(opt);
        customerReviewService.saveReview(review);

        caseService.saveCase(review.getAcase());

        return "redirect:/myOverview?borrowed";
    }

    /**
     * Provides a for springboot method to correctly receive and connect Article.category.
     */
    @InitBinder
    public void initBinder(final WebDataBinder webDataBinder) {
        webDataBinder.registerCustomEditor(Category.class, new CategoryConverter());
    }

    private static class CategoryConverter extends PropertyEditorSupport {

        @Override
        public void setAsText(final String text) {
            setValue(Category.fromValue(text));
        }
    }
}

package de.propra2.ausleiherino24.web;

import de.propra2.ausleiherino24.data.CaseRepository;
import de.propra2.ausleiherino24.data.CustomerReviewRepository;
import de.propra2.ausleiherino24.model.Article;
import de.propra2.ausleiherino24.model.Case;
import de.propra2.ausleiherino24.model.Category;
import de.propra2.ausleiherino24.model.CustomerReview;
import de.propra2.ausleiherino24.model.User;
import de.propra2.ausleiherino24.service.ArticleService;
import de.propra2.ausleiherino24.service.CaseService;
import de.propra2.ausleiherino24.service.ImageService;
import de.propra2.ausleiherino24.service.UserService;
import java.beans.PropertyEditorSupport;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;


@Controller
public class CaseController {

    private final ArticleService articleService;
    private final ImageService imageService;
    private final UserService userService;
    private final CaseService caseService;
    private final CustomerReviewRepository customerReviewRepository;
    private final CaseRepository caseRepository;

    private final List<Category> allCategories = Category.getAllCategories();

    /**
     * Manages all requests regarding creating/editing/deleting articles/cases and after-sales.
     * Possible features: transaction rating (karma/voting), chatting. TODO JavaDoc-Descriptions.
     * @param articleService Descriptions
     * @param userService Descriptions
     * @param imageService Descriptions
     * @param caseService Descriptions
     * @param customerReviewRepository
     * @param caseRepository
     */
    @Autowired
    public CaseController(ArticleService articleService,
            UserService userService,
            ImageService imageService,
            CaseService caseService,
            CustomerReviewRepository customerReviewRepository,
            CaseRepository caseRepository) {
        this.articleService = articleService;
        this.userService = userService;
        this.imageService = imageService;
        this.caseService = caseService;
        this.customerReviewRepository = customerReviewRepository;
        this.caseRepository = caseRepository;
    }

    /**
     * TODO Javadoc.
     *
     * @param id Descriptions
     * @param principal Descriptions
     * @return Descriptions
     * @throws Exception Descriptions
     */
    @GetMapping("/article")
    @SuppressWarnings("Duplicates") // TODO Duplicate code
    public ModelAndView displayArticle(@RequestParam("id") Long id, Principal principal)
            throws Exception {
        Article article = articleService.findArticleById(id);
        User currentUser = userService.findUserByPrincipal(principal);

        ModelAndView mav = new ModelAndView("/shop/item");
        mav.addObject("article", article);
        mav.addObject("user", currentUser);
        mav.addObject("categories", allCategories);
        return mav;
    }

    /**
     * TODO Javadoc.
     *
     * @param principal Descriptions
     * @return Descriptions
     */
    @GetMapping("/newArticle")
    public ModelAndView createNewCaseAndArticle(Principal principal) {
        Article article = new Article();
        User currentUser = userService.findUserByPrincipal(principal);

        ModelAndView mav = new ModelAndView("/shop/newItem");
        mav.addObject("article", article);
        mav.addObject("user", currentUser);
        mav.addObject("categories", allCategories);
        return mav;
    }

    /**
     * TODO Javadoc.
     *
     * @param article Descriptions
     * @param result Descriptions
     * @param model Descriptions
     * @param image Descriptions
     * @param principal Descriptions
     * @return Descriptions
     */
    @PostMapping("/saveNewArticle")
    public ModelAndView saveNewCaseAndArticle(@ModelAttribute @Valid Article article,
            BindingResult result, Model model,
            @RequestParam("image") MultipartFile image, Principal principal) {
        User user = userService.findUserByPrincipal(principal);
        article.setActive(true);
        article.setOwner(user);
        article.setImage(imageService.store(image, null));
        article.setForRental(true);
        article.setActive(true);
        articleService.saveArticle(article, "Created");

        return new ModelAndView("redirect:/");
    }

    /**
     * TODO Javadoc.
     *
     * @param article Descriptions
     * @param result Descriptions
     * @param model Descriptions
     * @param image Descriptions
     * @param principal Descriptions
     * @return Descriptions
     */
    @PutMapping("/saveEditedArticle")
    public ModelAndView saveEditedCaseAndArticle(@ModelAttribute @Valid Article article,
            BindingResult result, Model model,
            @RequestParam("image") MultipartFile image, Principal principal) {

        article.setImage(imageService.store(image, null));
        articleService.saveArticle(article, "Updated");

        User currentUser = userService.findUserByPrincipal(principal);

        ModelAndView mav = new ModelAndView("/shop/item");
        mav.addObject("article", article);
        mav.addObject("user", currentUser);
        return mav;
    }

    @RequestMapping("/updateArticle")
    public String updateArticle(@RequestParam Long id, Article article) {
        articleService.updateArticle(id, article);
        return "redirect:/myOverview?articles&updatedarticle";
    }

    @RequestMapping("/deleteArticle")
    public String deleteArticle(@RequestParam Long id) throws Exception {
        if (articleService.deactivateArticle(id)) {
            return "redirect:/myOverview?articles&deletedarticle";
        } else {
            return "redirect:/myOverview?articles&deletionfailed";
        }
    }


    //NEED FOR JS DEVE PLS DO NOT DELETE
    @RequestMapping("/api/events")
    @ResponseBody
    public List<LocalDate> test() throws Exception {
       return caseService.findAllReservedDaysbyArticle((long) 3);
    }

    /**
     * TODO JavaDoc.
     *
     * @param id Description
     * @param startDate Description
     * @param endDate Description
     * @param principal Description
     * @return Description
     * @throws Exception Description
     */
    @PostMapping("/bookArticle")
    public String bookArticle(@RequestParam Long id, String startDate, String endDate,
            Principal principal) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        try {
            if(caseService.requestArticle(
                    id,
                    simpleDateFormat.parse(startDate).getTime(),
                    simpleDateFormat.parse(endDate).getTime(),
                    principal.getName())){
                return "redirect:/article?id=" + id+ "&success";
            } else {
                return "redirect:/article?id=" + id+ "&failed";
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //TODO: Show the user, whether die request was successful or not
        return "redirect:/article?id=" + id;
    }

    @PostMapping("/acceptCase")
    public String acceptCase(@RequestParam Long id) {
        switch(caseService.acceptArticleRequest(id)) {
            case 1:
                return "redirect:/myOverview?requests";
            case 2:
                return "redirect:/myOverview?requests&alreadyrented";
            case 3:
                return "redirect:/myOverview?requests&receiveroutofmoney";
            default:
                return  "redirect:/myOverview?requests&error";
        }
    }

    @PostMapping("/declineCase")
    public String declineCase(@RequestParam Long id) {
        caseService.declineArticleRequest(id);
        return "redirect:/myOverview?requests&declined";
    }

    @PostMapping("/acceptCaseReturn")
    public String acceptCaseReturn(@RequestParam Long id) {
        caseService.acceptCaseReturn(id);
        return "redirect:/myOverview?returned&successfullyreturned";
    }


    /**
     * Gets called, when someone creates a review
     */
    @PostMapping("/writeReview")
    public String writeReview(@RequestParam Long id, CustomerReview review) {
        review.setTimestamp(new Date().getTime());
        Optional<Case> opt = caseService.findCaseById(id);
        if (opt.isPresent()) {
            review.setACase(opt.get());
            customerReviewRepository.save(review);
            caseRepository.save(review.getACase());
        }
        System.out.println(review);
        return "redirect:/myOverview?borrowed";
    }

    /**
     * Liefert einen Methode für Springboot um das Feld Article.category korrekt zu empfangen und zu
     * verknüpfen.
     */
    @InitBinder
    public void initBinder(final WebDataBinder webDataBinder) {
        webDataBinder.registerCustomEditor(Category.class, new CategoryConverter());
    }

    private class CategoryConverter extends PropertyEditorSupport {

        public void setAsText(final String text) throws IllegalArgumentException {
            setValue(Category.fromValue(text));
        }
    }
}

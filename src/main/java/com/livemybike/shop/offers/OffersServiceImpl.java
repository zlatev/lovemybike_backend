package com.livemybike.shop.offers;

import com.livemybike.shop.images.*;
import com.livemybike.shop.offers.booking.Booking;
import com.livemybike.shop.offers.booking.BookingService;
import com.livemybike.shop.security.Account;
import com.livemybike.shop.security.AccountService;
import com.livemybike.shop.security.AuthorizationException;
import com.livemybike.shop.util.DateUtil;
import org.apache.commons.lang.Validate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OffersServiceImpl implements OffersService {

    private static final int PAGE_SIZE = 12;

    @Autowired
    private OffersRepo offersRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ImageRepo imageRepo;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BookingService bookingService;

    @Override
    public Page<OfferDto> listOffers(String genderFilter, String location, int pageNumber) {
        // TODO: find a way to build a dynamic criteria
        if (StringUtils.isEmpty(genderFilter) && StringUtils.isEmpty(location)) {
            // Not filter passed. Return everything.
            return convertToDtoList(offersRepo.findAll(getPage(pageNumber)));
        } else if (!StringUtils.isEmpty(genderFilter) && StringUtils.isEmpty(location)) {
            // only gender filter passed
            List<String> filters = getGenderFiltersList(genderFilter);
            return convertToDtoList(offersRepo.findByGenderIn(filters, getPage(pageNumber)));
        } else if (StringUtils.isEmpty(genderFilter) && !StringUtils.isEmpty(location)) {
            // only location filter passed
            return convertToDtoList(offersRepo.findByCityIgnoreCaseOrPostcodeIgnoreCase(location, location, getPage(pageNumber)));
        } else {
            // location and gender filter passed
            List<String> filters = getGenderFiltersList(genderFilter);
            return convertToDtoList(offersRepo.findByGenderAndLocation(filters, location, getPage(pageNumber)));
        }
    }

    private List<String> getGenderFiltersList(String genderFilter) {
        return genderFilter.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OfferDto createOffer(String title, String price, String gender, String description, String street,
                                String number, String postcode, String city, MultipartFile... images) {
        Offer offer = new Offer();

        if (images == null || images.length == 0 || images.length > 6) {
            throw new IllegalArgumentException("Images should be between 1 and 6");
        }

        Account loggedInAccount = accountService.getCurrentLoggedIn();

        offer.setOwner(loggedInAccount);
        offer.setTitle(title);
        offer.setPrice(new BigDecimal(price));
        offer.setGender(gender);
        offer.setDescription(description);
        offer.setStreet(street);
        offer.setNumber(number);
        offer.setPostcode(postcode);
        offer.setCity(city);

        offer.setImage0(images[0].getOriginalFilename());

        if (images[1] != null) {
            offer.setImage1(images[1].getOriginalFilename());
        }
        if (images[2] != null) {
            offer.setImage2(images[2].getOriginalFilename());
        }
        if (images[3] != null) {
            offer.setImage3(images[3].getOriginalFilename());
        }
        if (images[4] != null) {
            offer.setImage4(images[4].getOriginalFilename());
        }
        if (images[5] != null) {
            offer.setImage5(images[5].getOriginalFilename());
        }

        Offer result = offersRepo.save(offer);

        storeImages(result.getId(), images);

        return convertToDto(result);
    }

    @Override
    public Page<OfferDto> listMyOffers(int pageNumber) {
        Account loggedInAccount = accountService.getCurrentLoggedIn();
        Page<Offer> offers = offersRepo.findByOwner(loggedInAccount, getPage(pageNumber));
        return convertToDtoList(offers);
    }

    @Override
    public List<Booking> getOfferBookings(long offerId) {
        Offer offer = offersRepo.findOne(offerId);
        if (offer == null) {
            throw new IllegalArgumentException(String.format("Offer with ID: %d not found", offerId));
        }
        Account currentUser = accountService.getCurrentLoggedIn();
        if (!offer.getOwner().equals(currentUser) ) {
            throw new AuthorizationException("The offer is not owned by the user");
        }
        return offer.getBookings();
    }

    @Override
    public OfferDto getOffer(long offerId) {
        Offer offer = offersRepo.findOne(offerId);
        return convertToDto(offer);
    }

    public List<Date> getBookedDaysForInterval(Long offerId, Date startDate, Date endDate) {
        Offer offer = offersRepo.findOne(offerId);
        Validate.notNull(offer, String.format("Offer with ID: %d not found", offerId));
        Validate.notNull(startDate,"startDate can not be null");
        Validate.notNull(endDate, "endDate can not be null");

        List<Booking> bookings = bookingService.findApprovedBookingByOfferInInterval(offer, startDate, endDate);

        List<Date> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        bookings.forEach(booking -> {
            Date startIntervalDate = DateUtil.getBeginningOfDay(DateUtil.getLate(booking.getFromDate(), startDate));
            Date endIntervalDate = DateUtil.getEndOfDay(DateUtil.getEarly(booking.getTo(), endDate));
            while (startIntervalDate.before(endIntervalDate)) {
                result.add(startIntervalDate);
                cal.setTime(startIntervalDate);
                cal.add(Calendar.DATE, 1);
                startIntervalDate = cal.getTime();
            }
        });
        return result;
    }

    private OfferDto convertToDto(Offer offer) {
        OfferDto result = modelMapper.map(offer, OfferDto.class);
        result.setOwner_id(offer.getOwner().getId());
        result.setOwnerName(offer.getOwner().getName());

        result.setImage0_s(ImageRepo.BASE_IMAGE_URL.concat(smallImageName(offer, offer.getImage0())));
        result.setImage0_m(ImageRepo.BASE_IMAGE_URL.concat(mediumImageName(offer, offer.getImage0())));

        if (!StringUtils.isEmpty(offer.getImage1())) {
            result.setImage1_s(ImageRepo.BASE_IMAGE_URL.concat(smallImageName(offer, offer.getImage1())));
            result.setImage1_m(ImageRepo.BASE_IMAGE_URL.concat(mediumImageName(offer, offer.getImage1())));
        }

        if (!StringUtils.isEmpty(offer.getImage2())) {
            result.setImage2_s(ImageRepo.BASE_IMAGE_URL.concat(smallImageName(offer, offer.getImage2())));
            result.setImage2_m(ImageRepo.BASE_IMAGE_URL.concat(mediumImageName(offer, offer.getImage2())));
        }

        if (!StringUtils.isEmpty(offer.getImage3())) {
            result.setImage3_s(ImageRepo.BASE_IMAGE_URL.concat(smallImageName(offer, offer.getImage3())));
            result.setImage3_m(ImageRepo.BASE_IMAGE_URL.concat(mediumImageName(offer, offer.getImage3())));
        }

        if (!StringUtils.isEmpty(offer.getImage4())) {
            result.setImage4_s(ImageRepo.BASE_IMAGE_URL.concat(smallImageName(offer, offer.getImage4())));
            result.setImage4_m(ImageRepo.BASE_IMAGE_URL.concat(mediumImageName(offer, offer.getImage4())));
        }

        if (!StringUtils.isEmpty(offer.getImage5())) {
            result.setImage5_s(ImageRepo.BASE_IMAGE_URL.concat(smallImageName(offer, offer.getImage5())));
            result.setImage5_m(ImageRepo.BASE_IMAGE_URL.concat(mediumImageName(offer, offer.getImage5())));
        }

        return result;
    }

    private String smallImageName(Offer offer, String imageName) {
        return ImageUtil.buildImageName(offer.getId(), ImageSize.SMALL.getValue(), imageName);
    }

    private String mediumImageName(Offer offer, String imageName) {
        return ImageUtil.buildImageName(offer.getId(), ImageSize.MEDIUM.getValue(), imageName);
    }

    private void storeImages(Long offerId, MultipartFile... images) {
        Arrays.stream(images).forEach(image -> {
            if (image != null) {
                try {
                    resizeAndStoreToS3(offerId, image);
                } catch (IOException e) {
                    throw new ImageStoringException("Can not upload the images", e);
                }
            }
        });
    }

    private void resizeAndStoreToS3(Long offerId, MultipartFile imageFile) throws IOException {
        Image image = new Image(imageFile.getInputStream(), imageFile.getOriginalFilename(),
                imageFile.getSize(), imageFile.getContentType());

        imageRepo.save(image.resizeToSmall(), offerId);
        imageRepo.save(image.resizeToMedium(), offerId);
    }

    private PageRequest getPage(int pageNumber) {
        // TODO: think about cache
        return new PageRequest(pageNumber - 1, PAGE_SIZE, Sort.Direction.DESC, "id");
    }

    private Page<OfferDto> convertToDtoList(Page<Offer> offerPage) {
        return offerPage.map(this::convertToDto);
    }

}

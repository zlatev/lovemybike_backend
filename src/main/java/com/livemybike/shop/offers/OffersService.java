package com.livemybike.shop.offers;

import com.livemybike.shop.offers.booking.Booking;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface OffersService {

    /**
     * Search offers by particular criteria
     *
     * @param genderFilter
     *            Concatenated desired gender chars
     * @param location
     *            Postcode or city
     * @param pageNumber
     * @return offers that fits to the desired criteria
     */
    Page<OfferDto> listOffers(String genderFilter, String location, int pageNumber);

    OfferDto createOffer(String title, String price, String gender, String description, String street,
                         String number, String postcode, String city, MultipartFile... image0);

    Page<OfferDto> listMyOffers(int pageNumber);

    List<Booking> getOfferBookings(long offerId);

    OfferDto getOffer(long offerId);

    List<Date> getBookedDaysForInterval(Long offerId, Date startDate, Date endDate);
}

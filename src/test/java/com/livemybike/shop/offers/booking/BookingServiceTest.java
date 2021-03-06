package com.livemybike.shop.offers.booking;

import com.livemybike.shop.offers.Offer;
import com.livemybike.shop.offers.OffersRepo;
import com.livemybike.shop.security.Account;
import com.livemybike.shop.security.AccountRepo;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class BookingServiceTest extends AbstractBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private OffersRepo offersRepo;

    @Autowired
    private BookingsRepo bookingsRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private ApplicationContext context;

    public void authenticate(String user, String pass) {
        AuthenticationManager authenticationManager = this.context
                .getBean(AuthenticationManager.class);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user, pass));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @After
    public void close() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void requestBookingTest() throws InvalidBookingException {
        Offer offer = offersRepo.findOne(1L);
        Account requestedBy = accountRepo.findOne(2L);
        Date from = getFrom();
        Date to = getTo();
        Booking booking = new Booking(from, to, offer, requestedBy);

        Booking savedBooking = bookingService.requestBooking(booking);

        assertThat(savedBooking.getId(), is(greaterThan(0L)));
        assertThat(savedBooking.getState(), equalTo(State.REQUEST_STATE));
    }

    @Test
    public void request2BookingSameIntervalTest() throws InvalidBookingException {
        Offer offer = offersRepo.findOne(1L);
        Account requestedBy2 = accountRepo.findOne(2L);
        Date from = getFrom();
        Date to = getTo();
        Booking booking1 = new Booking(from, to, offer, requestedBy2);

        Account requestedBy3 = accountRepo.findOne(3L);
        Booking booking2 = new Booking(from, to, offer, requestedBy3);

        Booking savedBooking1 = bookingService.requestBooking(booking1);
        Booking savedBooking2 = bookingService.requestBooking(booking2);

        assertThat(savedBooking1.getId(), is(greaterThan(0L)));
        assertThat(savedBooking2.getId(), is(greaterThan(0L)));
    }

    @Test(expected = InvalidBookingException.class)
    public void requestBookingSameIntervalApprovedBookingExist1Test() throws InvalidBookingException {
        Offer offer = offersRepo.findOne(1L);
        Account requestedBy2 = accountRepo.findOne(2L);
        Date from1 = getDate(3);
        Date to1 = getDate(7);
        Booking booking1 = new Booking(from1, to1, offer, requestedBy2);
        booking1.setState(State.APPROVED_STATE);
        bookingsRepo.save(booking1);

        Date from2 = getDate(2);
        Date to2 = getDate(5);
        Account requestedBy3 = accountRepo.findOne(3L);
        Booking booking2 = new Booking(from2, to2, offer, requestedBy3);

        bookingService.requestBooking(booking2);
    }

    @Test(expected = InvalidBookingException.class)
    public void requestBookingSameIntervalApprovedBookingExist2Test() throws InvalidBookingException {
        Offer offer = offersRepo.findOne(1L);
        Account requestedBy2 = accountRepo.findOne(2L);
        Date from1 = getDate(2);
        Date to1 = getDate(5);
        Booking booking1 = new Booking(from1, to1, offer, requestedBy2);
        booking1.setState(State.APPROVED_STATE);
        bookingsRepo.save(booking1);

        Date from2 = getDate(3);
        Date to2 = getDate(7);
        Account requestedBy3 = accountRepo.findOne(3L);
        Booking booking2 = new Booking(from2, to2, offer, requestedBy3);

        bookingService.requestBooking(booking2);
    }

    @Test(expected = InvalidBookingException.class)
    public void requestBookingSameIntervalApprovedBookingExist3Test() throws InvalidBookingException {
        Offer offer = offersRepo.findOne(1L);
        Account requestedBy2 = accountRepo.findOne(2L);
        Date from1 = getDate(4);
        Date to1 = getDate(5);
        Booking booking1 = new Booking(from1, to1, offer, requestedBy2);
        booking1.setState(State.APPROVED_STATE);
        bookingsRepo.save(booking1);

        Date from2 = getDate(3);
        Date to2 = getDate(7);
        Account requestedBy3 = accountRepo.findOne(3L);
        Booking booking2 = new Booking(from2, to2, offer, requestedBy3);

        bookingService.requestBooking(booking2);
    }

    @Test(expected = InvalidBookingException.class)
    public void requestBookingSameIntervalApprovedBookingExist4Test() throws InvalidBookingException {
        Offer offer = offersRepo.findOne(1L);
        Account requestedBy2 = accountRepo.findOne(2L);
        Date from1 = getDate(3);
        Date to1 = getDate(7);
        Booking booking1 = new Booking(from1, to1, offer, requestedBy2);
        booking1.setState(State.APPROVED_STATE);
        bookingsRepo.save(booking1);

        Date from2 = getDate(4);
        Date to2 = getDate(5);
        Account requestedBy3 = accountRepo.findOne(3L);
        Booking booking2 = new Booking(from2, to2, offer, requestedBy3);

        bookingService.requestBooking(booking2);
    }

    @Test
    public void approveBookingTest() throws InvalidBookingException, InvalidStateTransitionException {
        Offer offer = offersRepo.findOne(1L);
        authenticate(offer.getOwner().getEmail(), offer.getOwner().getPassword());
        Date from = getDate(4);
        Date to = getDate(5);
        Account requestedBy = accountRepo.findOne(2L);
        Booking booking = new Booking(from, to, offer, requestedBy);

        Booking savedBooking = bookingService.requestBooking(booking);
        bookingService.approveBooking(savedBooking.getId());

        Booking approvedBooking = bookingsRepo.findOne(savedBooking.getId());

        assertThat(approvedBooking.getState(), equalTo(State.APPROVED_STATE));
    }

    @Test
    public void cancelRequestedBookingTest() throws InvalidBookingException, InvalidStateTransitionException {
        Offer offer = offersRepo.findOne(1L);
        authenticate(offer.getOwner().getEmail(), offer.getOwner().getPassword());
        Date from = getDate(4);
        Date to = getDate(5);
        Account requestedBy = accountRepo.findOne(2L);
        Booking booking = new Booking(from, to, offer, requestedBy);

        Booking savedBooking = bookingService.requestBooking(booking);
        bookingService.cancelBooking(savedBooking.getId());

        Booking approvedBooking = bookingsRepo.findOne(savedBooking.getId());

        assertThat(approvedBooking.getState(), equalTo(State.CANCELED_STATE));
    }

    @Test
    public void cancelApprovedBookingTest() throws InvalidBookingException, InvalidStateTransitionException {
        Offer offer = offersRepo.findOne(1L);
        authenticate(offer.getOwner().getEmail(), offer.getOwner().getPassword());
        Date from = getDate(4);
        Date to = getDate(5);
        Account requestedBy = accountRepo.findOne(2L);
        Booking booking = new Booking(from, to, offer, requestedBy);

        Booking savedBooking = bookingService.requestBooking(booking);
        bookingService.approveBooking(savedBooking.getId());
        bookingService.cancelBooking(savedBooking.getId());

        Booking approvedBooking = bookingsRepo.findOne(savedBooking.getId());

        assertThat(approvedBooking.getState(), equalTo(State.CANCELED_STATE));
    }

}

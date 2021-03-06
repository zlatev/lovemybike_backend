package com.livemybike.shop.offers.booking;

import com.livemybike.shop.offers.Offer;
import com.livemybike.shop.security.Account;
import com.livemybike.shop.util.DateUtil;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class BookingTest extends AbstractBookingTest {

    @Test
    public void defaultStateRequestTest() {
        Booking booking = new Booking();
        assertThat(booking.getState().getValue(), equalTo(State.REQUEST_STATE_STRING));
    }

    @Test
    public void requestBookingTest() {
        Date from = getFrom();
        Date to = getTo();
        Offer offer = getOffer(getOfferOwner());
        Account requestedBy = getRequestedBy();

        Booking booking = new Booking(from, to, offer, requestedBy);

        assertThat(booking.getState().getValue(), equalTo(State.REQUEST_STATE_STRING));
        assertThat(booking.getFromDate(), equalTo(DateUtil.getBeginningOfDay(from)));
        assertThat(booking.getTo(), equalTo(DateUtil.getEndOfDay(to)));
        assertThat(booking.getOffer(), equalTo(offer));
        assertThat(booking.getRequestedBy(), equalTo(requestedBy));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requestBookingToNotBeforeFrom() {
        Date to = getFrom();
        Date from = getTo();
        Offer offer = getOffer(getOfferOwner());
        Account requestedBy = getRequestedBy();

        new Booking(from, to, offer, requestedBy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requestBookingFromInThePastNotAllowed() {
        Date to = getFromPast();
        Date from = getTo();
        Offer offer = getOffer(getOfferOwner());
        Account requestedBy = getRequestedBy();

        new Booking(from, to, offer, requestedBy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bookingMustHaveFromDate() {
        Date to = getTo();
        Offer offer = getOffer(getOfferOwner());
        Account requestedBy = getRequestedBy();

        new Booking(null, to, offer, requestedBy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bookingMustHaveToDate() {
        Date from = getFrom();
        Offer offer = getOffer(getOfferOwner());
        Account requestedBy = getRequestedBy();

        new Booking(from, null, offer, requestedBy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bookingMustHaveOffer() {
        Date from = getFrom();
        Date to = getTo();
        Account requestedBy = getRequestedBy();

        new Booking(from, to, null, requestedBy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bookingMustHaveRequestedBy() {
        Date from = getFrom();
        Date to = getTo();
        Offer offer = getOffer(getOfferOwner());

        new Booking(from, to, offer, null);
    }

    @Test
    public void ownerApproveOffer() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());

        assertThat(booking.getState().getValue(), equalTo(State.APPROVED_STATE_STRING));
    }

    @Test(expected = InvalidStateTransitionException.class)
    public void somebodyElseApproveOfferNotAllowed() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        try {
            booking.approve(getSomebodyElse());
        } catch (InvalidStateTransitionException e) {
            assertThat(booking.getState().getValue(), equalTo(State.REQUEST_STATE_STRING));
            throw e;
        }

    }

    @Test(expected = InvalidStateTransitionException.class)
    public void requestedByApproveOfferNotAllowed() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        try {
            booking.approve(getRequestedBy());
        } catch (InvalidStateTransitionException e) {
            assertThat(booking.getState().getValue(), equalTo(State.REQUEST_STATE_STRING));
            throw e;
        }

    }

    @Test
    public void ownerCancelOffer() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.cancel(getOfferOwner());

        assertThat(booking.getState().getValue(), equalTo(State.CANCELED_STATE_STRING));
    }

    @Test
    public void requestedByCancelOffer() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.cancel(getRequestedBy());

        assertThat(booking.getState().getValue(), equalTo(State.CANCELED_STATE_STRING));
    }

    @Test(expected = InvalidStateTransitionException.class)
    public void somebodyElseCancelOfferNotAllowed() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        try {
            booking.cancel(getSomebodyElse());
        } catch (InvalidStateTransitionException e) {
            assertThat(booking.getState().getValue(), equalTo(State.REQUEST_STATE_STRING));
            throw e;
        }

    }

    @Test
    public void ownerCancelApprovedOffer() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());
        booking.cancel(getOfferOwner());

        assertThat(booking.getState().getValue(), equalTo(State.CANCELED_STATE_STRING));
    }

    @Test
    public void requestedByCancelApprovedOffer() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());
        booking.cancel(getRequestedBy());

        assertThat(booking.getState().getValue(), equalTo(State.CANCELED_STATE_STRING));
    }

    @Test(expected = InvalidStateTransitionException.class)
    public void somebodyElseCancelApprovedOfferNotAllowed() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());
        try {
            booking.cancel(getSomebodyElse());
        } catch (InvalidStateTransitionException e) {
            assertThat(booking.getState().getValue(), equalTo(State.APPROVED_STATE_STRING));
            throw e;
        }

    }

    @Test
    public void requestedByReopenCanceledOffer() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());
        booking.cancel(getRequestedBy());
        booking.reopen(getRequestedBy());

        assertThat(booking.getState().getValue(), equalTo(State.REQUEST_STATE_STRING));
    }

    @Test(expected = InvalidStateTransitionException.class)
    public void ownerReopenCanceledOfferNotAllowed() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());
        booking.cancel(getRequestedBy());
        try {
            booking.reopen(getOfferOwner());
        } catch (InvalidStateTransitionException e) {
            assertThat(booking.getState().getValue(), equalTo(State.CANCELED_STATE_STRING));
            throw e;
        }

    }

    @Test(expected = InvalidStateTransitionException.class)
    public void somebodyElseCanceledOfferNotAllowed() throws InvalidStateTransitionException {
        Booking booking = createValidBooking();
        booking.approve(getOfferOwner());
        booking.cancel(getRequestedBy());
        try {
            booking.reopen(getSomebodyElse());
        } catch (InvalidStateTransitionException e) {
            assertThat(booking.getState().getValue(), equalTo(State.CANCELED_STATE_STRING));
            throw e;
        }

    }


}

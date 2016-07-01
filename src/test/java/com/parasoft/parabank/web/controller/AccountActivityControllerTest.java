package com.parasoft.parabank.web.controller;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.Test;
import org.reflections.util.Utils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import com.parasoft.parabank.domain.Transaction;
import com.parasoft.parabank.domain.TransactionCriteria;
import com.parasoft.parabank.util.Constants;

@SuppressWarnings({ "unchecked" })
public class AccountActivityControllerTest extends AbstractBankControllerTest<AccountActivityController> {
    private void assertInvalidRequest(final boolean get) throws Exception {
        ModelAndView mav = null;
        try {
            if (get) {
                mav = processGetRequest(request, response);
            } else {
                mav = processPostRequest(getTransactonCriteriaForm(null), request, response);
            }
            fail("expected exception (MissingServletRequestParameterException) not thrown");
        } catch (final Exception ex) {
            assertEquals("Required String parameter 'id' is not present", ex.getMessage());
            // this is good
        }
        //        assertEquals("error", mav.getViewName());
        //        assertEquals("error.missing.account.id", getModelValue(mav, "message"));

        request = registerSession(new MockHttpServletRequest());
        String id = "str";
        request.setParameter("id", id);
        if (get) {
            mav = processGetRequest(request, response);
        } else {
            mav = processPostRequest(getTransactonCriteriaForm(id), request, response);
        }
        assertEquals("error", mav.getViewName());
        assertEquals("error.invalid.account.id", getModelValue(mav, "message"));

        request = registerSession(new MockHttpServletRequest());
        id = "0";
        request.setParameter("id", id);
        if (get) {
            mav = processGetRequest(request, response);
        } else {
            mav = processPostRequest(getTransactonCriteriaForm(id), request, response);
        }
        assertEquals("error.invalid.account.id", getModelValue(mav, "message"));
    }

    private void assertReferenceData(final ModelAndView mav) {
        final List<String> months = (List<String>) mav.getModel().get("months");
        assertEquals(13, months.size());

        final List<String> types = (List<String>) mav.getModel().get("types");
        assertEquals(3, types.size());
    }

    private void assertTransactions(final int expectedSize, final String id) throws Exception {
        final ModelAndView mav =
            processPostRequest(getTransactonCriteriaForm(id), request, new MockHttpServletResponse());
        final List<Transaction> transactions = (List<Transaction>) getModelValue(mav, "transactions");
        assertEquals(expectedSize, transactions.size());
        assertReferenceData(mav);
    }

    /**
     * <DL>
     * <DT>Description:</DT>
     * <DD>retrieve the TransactionCriteriaFrom from the Controller</DD>
     * <DT>Date:</DT>
     * <DD>Oct 19, 2015</DD>
     * </DL>
     *
     * @param id
     *            the account id to use to load the list of transactions
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws Exception
     */
    public TransactionCriteria getTransactonCriteriaForm(final String id)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, Exception {
        final MockHttpServletRequest lrequest = registerSession(new MockHttpServletRequest());
        if (!Utils.isEmpty(id)) {
            lrequest.setParameter("id", id);
        }
        final ModelAndView mav = processGetRequest(lrequest, new MockHttpServletResponse());
        final TransactionCriteria form = (TransactionCriteria) mav.getModel().get(getFormName());
        return form;
    }

    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        setPath("/activity.htm");
        setFormName(Constants.TRANSACTIONCRITERIA);
        registerSession(request);
        //controller.setCommandClass(TransactionCriteria.class);
    }

    @Test
    public void testHandleGetRequest() throws Exception {
        request.setParameter("id", "12345");
        final ModelAndView mav = processGetRequest(request, response);
        final List<Transaction> transactions = (List<Transaction>) getModelValue(mav, "transactions");
        assertEquals(7, transactions.size());
        assertReferenceData(mav);
    }

    @Test
    public void testHandleInvalidGetRequest() throws Exception {
        assertInvalidRequest(true);
    }

    @Test
    public void testHandleInvalidPostRequest() throws Exception {
        assertInvalidRequest(false);
    }

    @Test
    @Transactional
    @Rollback
    public void testHandlePostRequest() throws Exception {
        final String id = "12345";
        request.setParameter("id", id);
        assertTransactions(7, id);

        request = registerSession(new MockHttpServletRequest());
        request.setParameter("id", id);
        request.setParameter("transactionType", "Credit");
        assertTransactions(1, id);

        request = registerSession(new MockHttpServletRequest());
        request.setParameter("id", id);
        request.setParameter("transactionType", "Debit");
        assertTransactions(6, id);

        request = registerSession(new MockHttpServletRequest());
        request.setParameter("id", id);
        request.setParameter("transactionType", "All");
        request.setParameter("Month", "December");
        assertTransactions(2, id);

        request = registerSession(new MockHttpServletRequest());
        request.setParameter("id", id);
        request.setParameter("transactionType", "All");
        request.setParameter("Month", "All");
        assertTransactions(7, id);
    }
}
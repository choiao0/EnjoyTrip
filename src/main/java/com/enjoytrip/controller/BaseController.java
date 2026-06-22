package com.enjoytrip.controller;

import com.enjoytrip.model.DraftPlan;
import com.enjoytrip.model.User;
import jakarta.servlet.http.HttpSession;

public abstract class BaseController {

    private static final String SESSION_LOGIN_USER = "loginUser";
    private static final String SESSION_DRAFT_PLAN = "draftPlan";

    protected User currentUser(HttpSession session) {
        return (User) session.getAttribute(SESSION_LOGIN_USER);
    }

    protected boolean isLoggedIn(HttpSession session) {
        return currentUser(session) != null;
    }

    protected DraftPlan draftPlan(HttpSession session) {
        DraftPlan draft = (DraftPlan) session.getAttribute(SESSION_DRAFT_PLAN);
        if (draft == null) {
            draft = new DraftPlan();
            session.setAttribute(SESSION_DRAFT_PLAN, draft);
        }
        return draft;
    }

    protected void setLoginUser(HttpSession session, User user) {
        session.setAttribute(SESSION_LOGIN_USER, user);
    }

    protected void clearSession(HttpSession session) {
        session.removeAttribute(SESSION_LOGIN_USER);
        session.removeAttribute(SESSION_DRAFT_PLAN);
    }
}

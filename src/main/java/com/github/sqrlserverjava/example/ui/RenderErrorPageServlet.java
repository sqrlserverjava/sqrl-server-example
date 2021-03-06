package com.github.sqrlserverjava.example.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.sqrlserverjava.example.ErrorId;

@WebServlet(urlPatterns = { "/error" })
public class RenderErrorPageServlet extends HttpServlet {
	private static final long serialVersionUID = -5354194555510032618L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
	}
}

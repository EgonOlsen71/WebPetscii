package de.sixtyfour.petscii.server;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author EgonOlsen
 */
@WebServlet(name = "Convert", urlPatterns = { "/Convert" }, initParams = {
        @WebInitParam(name = "uploadpath", value = "/uploadimg/") })
public class Converter extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public Converter() {
        // TODO Auto-generated constructor stub
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }
}

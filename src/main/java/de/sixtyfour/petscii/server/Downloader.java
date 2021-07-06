package de.sixtyfour.petscii.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author EgonOlsen
 */
@WebServlet(name = "Download", urlPatterns = { "/Download" }, initParams = {
		@WebInitParam(name = "uploadpath", value = "/uploadimg/") })
public class Downloader extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public Downloader() {
		// TODO Auto-generated constructor stub
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (request.getParameter("preview") != null) {
			doPreview(request, response);
			return;
		}
		String file = request.getParameter("file");
		if (file.contains("..") || file.contains("\\") || file.startsWith("/")) {
			Logger.log("Invalid file name: " + file);
			return;
		}

		if (!file.endsWith(".zip")) {
			Logger.log("Invalid file name: " + file);
			return;
		}

		response.setContentType("application/octet-stream");
		response.setHeader("Content-disposition", "attachment; filename="+ file.substring(file.indexOf("/")+1));
		sendFile(response, file);
	}

	private void doPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String file = request.getParameter("preview");
		if (file.contains("..") || !file.startsWith("prev/")) {
			Logger.log("Invalid image name: " + file);
			return;
		}

		if (!file.endsWith(".png")) {
			Logger.log("Invalid image name: " + file);
			return;
		}

		response.setContentType("image/png");
		sendFile(response, file);
	}

	private void sendFile(HttpServletResponse response, String file) throws IOException {
		ServletOutputStream os = response.getOutputStream();
		ServletConfig sc = getServletConfig();
		String path = sc.getInitParameter("uploadpath");

		File bin = new File(path + file);
		byte[] buffer = new byte[8192];
		int len = 0;
		try (FileInputStream fis = new FileInputStream(bin)) {
			while ((len = fis.read(buffer)) > -1) {
				os.write(buffer, 0, len);
			}
		} catch (Exception e) {
			Logger.log("Failed to transfer file: " + file, e);
		} finally {
			delete(bin);
		}
	}

	private void delete(File bin) {
		Logger.log("Deleting file: "+bin);
		boolean ok1=bin.delete();
		boolean ok2=bin.getParentFile().delete();
		Logger.log("Status: "+ok1+"/"+ok2);
	}
}
package de.sixtyfour.petscii.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author EgonOlsen
 */
@WebServlet(name = "Upload", urlPatterns = { "/Upload" }, initParams = {
		@WebInitParam(name = "uploadpath", value = "/uploadimg/") })
@MultipartConfig
public class Uploader extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final static long MAX_FILE_SIZE=1024*1024*2;
	
	public final static String[] ALLOWED_EXTENSIONS = { ".jpg", ".png", ".jpeg", "webp" };


	public Uploader() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		ServletOutputStream os = response.getOutputStream();

		try {
			ServletConfig sc = getServletConfig();
			String path = sc.getInitParameter("uploadpath");
			new File(path).mkdirs();
			Part filePart = request.getPart("image");

			String fileName = filePart.getSubmittedFileName();
			InputStream is = filePart.getInputStream();

			Logger.log("Uploading image: " + fileName);

			checkFile(fileName);

			fileName = fileName.replace(" ", "-").replace("&", "-");
			fileName = (int)(Math.random()*10000)+""+System.currentTimeMillis() + "_" + fileName;
			Path target = Paths.get(path + fileName);
			Logger.log("Copying file to: " + target);

			Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
			
			if (target.toFile().length()==0) {
				Logger.log("Upload failed, file size mismatch: " + fileName);
				target.toFile().delete();
				returnError(os);
			}

			if (target.toFile().length()>MAX_FILE_SIZE) {
				Logger.log("Upload failed, file too large: " + fileName);
				target.toFile().delete();
				returnError(os, "Max. file size is "+MAX_FILE_SIZE+" bytes!");
			}
			
			Logger.log("Upload ok: " + fileName);
			returnOk(fileName, os);
		} catch (Exception e) {
			Logger.log("Upload failed!", e);
			returnError(os);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String fileName = request.getParameter("url");
		checkFile(fileName);
		Logger.log("Deleting file: " + fileName);

		ServletConfig sc = getServletConfig();
		String path = sc.getInitParameter("uploadpath");
		Path target = Paths.get(path + fileName);
		boolean deleted = target.toFile().delete();
		if (!deleted) {
			target.toFile().deleteOnExit();
		}
		Logger.log("File " + fileName + " deleted: " + deleted);
	}

	private void checkFile(String fileName) throws IOException {
		if (fileName.contains("\\") || fileName.contains("/") || fileName.contains("..")) {
			Logger.log("Invalid file name: " + fileName);
			throw new IOException("Invalid file name: " + fileName);
		}
		
		String file = fileName.toLowerCase(Locale.ENGLISH);
		boolean ok = false;
		for (String ext : ALLOWED_EXTENSIONS) {
			if (file.endsWith(ext)) {
				ok = true;
				break;
			}
		}
		if (!ok) {
			Logger.log("Invalid file extension: " + fileName);
			throw new IOException("Invalid file extension: " + fileName);
		}
	}

	private void returnOk(String name, ServletOutputStream os) throws Exception {
		JsonResult res = new JsonResult();
		res.setText(name);
		res.setType("ok");

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writeValue(os, res);
	}

	private void returnError(ServletOutputStream os) {
		returnError(os, null);
	}

	private void returnError(ServletOutputStream os, String txt) {
		try {
			JsonResult res = new JsonResult();
			res.setType("error");
			res.setText(txt);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.writeValue(os, res);
		} catch (Exception e) {
			Logger.log("Failed to serialize object!", e);
		}
	}

}

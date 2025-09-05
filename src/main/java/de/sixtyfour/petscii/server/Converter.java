package de.sixtyfour.petscii.server;

import com.sixtyfour.petscii.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        request.setCharacterEncoding("UTF-8");
        Parameters params=readParameters(request);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain");

        ServletOutputStream os = response.getOutputStream();
        ServletConfig sc = getServletConfig();
        String path = sc.getInitParameter("uploadpath");

        String[] files = request.getParameterValues("filelist");
        if (files==null || files.length==0) {
            files = request.getParameterValues("filelist[]");
        }
        List<String> res = new ArrayList<>();
        boolean ok = false;

        ok = convert(params, path, files, os, res);

        delete(path, files, ok ? null : res, os);

        if (res.size() > 0 && ok) {
            String name = res.get(0);
            if (res.size() > 1) {
                name = zipFiles(res, os);
            }
            setMarking(os);
            if (name != null) {
                name = name.replace(path, "");
                os.println(URLEncoder.encode(name, "UTF-8"));
            } else {
                setMarking(os);
                os.println("error");
            }
        } else {
            setMarking(os);
            os.println("error");
        }
    }

    private String zipFiles(List<String> res, ServletOutputStream os) throws IOException {
        String dir = res.get(0).substring(0, res.get(0).lastIndexOf("/") + 1);
        String zipName = dir + "files.zip";
        byte[] buffer = new byte[8192];

        out("Zipping files...");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName))) {
            int cnt = 0;
            String lastDir = null;
            Set<String> used = new HashSet<>();
            for (String file : res) {
                out("Processing file: " + file);

                int pos = file.lastIndexOf("/");
                String entryName = file.substring(pos + 1);
                String dirName = file.substring(0, pos);
                File dirDir = new File(dirName);

                if (used.contains(entryName)) {
                    pos = entryName.lastIndexOf(".");
                    entryName = entryName.substring(0, pos) + "-" + cnt + entryName.substring(pos);
                }

                used.add(entryName);

                out.putNextEntry(new ZipEntry(entryName));
                try (InputStream in = new FileInputStream(file)) {
                    int len;
                    while ((len = in.read(buffer)) > -1) {
                        out.write(buffer, 0, len);
                    }
                } catch (Exception e) {
                    Logger.log("Failed to create zip file!", e);
                    out("Failed to create zip file!");
                    return null;
                } finally {
                    File toDel = new File(file);
                    toDel.delete();
                    if (file.endsWith(".koa") || file.endsWith(".hed")) {
                        new File(file+".png").delete();
                    }
                    if (!dirName.equals(lastDir)) {
                        cnt++;
                        lastDir = dirName;
                    }
                    if (cnt > 1 && dirDir.listFiles().length == 0) {
                        toDel.getParentFile().delete();
                    }
                }
            }
        } catch (Exception e) {
            Logger.log("Failed to create zip file!", e);
            return null;
        }
        out(res.size() + " files zipped!");
        return zipName;
    }

    private void out(String text) {
        com.sixtyfour.petscii.Logger.log(text);
        Logger.log(text);
    }

    private boolean convert(Parameters params, String path, String[] files, ServletOutputStream os, List<String> res) {
        setupOutputStream(os);

        boolean tedMode = "264".equals(params.getPlatform());
        ColorMap colors = tedMode ? new TedColors() : new Vic2Colors();

        Logger.log("Using color map for: " + (tedMode ? "TED" : "VIC II"));

        List<String> formats = new ArrayList<>();
        formats.add("image");
        formats.add("basic");
        formats.add("bbs");
        formats.add("bin");

        Integer scale = params.getPrescale();
        scale = Math.max(1, Math.min(4, scale));

        Integer bgColor = params.getBackground();
        if (bgColor >=0 && !params.isBackgroundDefault()) {
            bgColor = Math.max(0, Math.min(tedMode ? 127 : 15, bgColor));
        } else {
            bgColor=null;
        }

        Boolean lowerCase = params.isLowerCase();
        Boolean noAlpha = params.isExcludeAlpha();

        int algorithm = params.getColorMapper();
        Algorithm algo = Algorithm.COLORFUL;
        if (algorithm==1) {
            algo = Algorithm.DITHERED;
        } else if (algorithm==2) {
            algo = Algorithm.SOFT;
        }

        for (String file : files) {
            long s = System.currentTimeMillis();
            if (file.contains("..") || file.contains("\\") || file.contains("/")) {
                out("Invalid file name: " + file);
                return false;
            }

            String srcFile = file.substring(file.indexOf("_") + 1);
            out("Converting " + srcFile);
            file = path + file;

            String targetDir = path + UUID.randomUUID() + "/";
            new File(targetDir).mkdirs();

            String targetFolder = targetDir;
            File folder = null;
            if (targetFolder != null && !targetFolder.isEmpty()) {
                folder = new File(targetFolder);
                if (folder.exists() && !folder.isDirectory()) {
                    out(folder + " isn't a directory!");
                    return false;
                }
                folder.mkdirs();
            }

            File pic = new File(file);
            File rawPic = new File(path, srcFile);

            try {
                String picName = pic.getName().toLowerCase();
                boolean matches = Arrays.asList(Uploader.ALLOWED_EXTENSIONS).stream().anyMatch(p -> picName.endsWith(p));
                if (!matches) {
                    out("Unsupported file format and/or extension: " + pic.getName());
                    continue;
                }
                Bitmap bitmap = new Bitmap(pic.toString(), scale);

                if (bgColor != null) {
                    bitmap.setBackgroundColor(bgColor);
                }
                bitmap.preprocess(algo, colors, 1);

                Petscii petscii = new Petscii(lowerCase);
                if (noAlpha) {
                    petscii.removeAlphanumericChars();
                }

                if (formats.contains("bbs")) {
                    petscii.removeControlCodes();
                }
                petscii.setSymbolBoost(params.getBoost());

                ConvertedData data = bitmap.convertToPetscii(8, false, petscii, tedMode);

                if (formats.contains("image")) {
                    String imgFile = Saver.savePetsciiImage(rawPic, bitmap, folder);
                    res.add(imgFile);
                    addPreview(path, imgFile, os);
                }
                if (formats.contains("basic")) {
                    res.add(Saver.savePetsciiBasicCode(rawPic, data, folder));
                }
                if (formats.contains("bbs")) {
                    res.add(Saver.savePetsciiBbs(rawPic, data, folder));
                }
                if (formats.contains("bin")) {
                    res.addAll(Arrays.asList(Saver.savePetsciiBin(rawPic, data, folder)));
                }

                out("Background color is: " + data.getBackGroundColor());

                if (params.isKoala()) {
                    long start = System.currentTimeMillis();
                    out("Koala conversion in progress...");
                    File koalaFile = Saver.createTempFileName(rawPic, folder, "koala.koa");
                    String koalaName = koalaFile.toString().replace("\\", "/");
                    KoalaConverter.convert(pic.toString(), koalaName, new Vic2Colors(), 1,  ((float) params.getKoalaDither()/100f), true, true);
                    res.add(koalaName);
                    addPreview(path, koalaName+".png", os);

                    File hiresFile = Saver.createTempFileName(rawPic, folder, "hires.hed");
                    String hiresName = hiresFile.toString().replace("\\", "/");
                    HiEddiConverter.convert(pic.toString(), hiresName, new Vic2Colors(), 1,  ((float) params.getKoalaDither()/100f), true, true);
                    res.add(hiresName);
                    addPreview(path, hiresName+".png", os);

                    out("Koala/Hires conversion done in "+(System.currentTimeMillis()-start)+"ms!");
                }

                out(srcFile + " converted in " + (System.currentTimeMillis() - s) + "ms!\n\n");
            } catch (Exception e) {
                out("Failed to process " + pic + ": " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    private void addPreview(String path, String imgFile, ServletOutputStream os) {
        int pos=imgFile.lastIndexOf("/");
        String oldFile=imgFile;
        if (pos!=-1) {
            new File(path+"prev/").mkdirs();
            String fileName="prev/prev_"+UUID.randomUUID()+"_"+imgFile.substring(pos+1).toLowerCase();
            imgFile=path+fileName;
            Path to = Paths.get(imgFile);
            Path from = Paths.get(oldFile);
            try {
                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                Logger.log("Copied file: "+fileName);
                // No idea why ISO-8859-1...makes no sense...
                os.print("~#~#"+new String(fileName.getBytes(StandardCharsets.UTF_8), "ISO-8859-1")+"~#~#\n");
                out("Created preview image: "+imgFile);
            } catch(Exception e) {
                Logger.log("Failed to copy file!", e);
                return;
            }
        }
    }

    private void setupOutputStream(ServletOutputStream os) {
        try {
            PrintStream ps = new PrintStream(new OutputStream() {
                private int cnt = 0;

                @Override
                public void write(int val) throws IOException {
                    os.write(val);
                    cnt++;
                    if (cnt >= 32 || (char) val == '\n') {
                        os.flush();
                        cnt = 0;
                    }
                }
            }, true, "UTF-8");
            com.sixtyfour.petscii.Logger.setThreadBoundPrintStream(ps);
        } catch(Exception e) {
            //
        }

        try {
            for (int i=0; i<40; i++) {
                // Force output...
                os.println("---------------------------------------");
            }
            os.flush();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void setMarking(ServletOutputStream os) throws IOException {
        os.println("~~~~");
    }

    private void delete(String path, String[] files, List<String> res, ServletOutputStream os) throws IOException {
        os.println("Deleting source files...");
        for (String file : files) {
            File fi = new File(path + file);
            boolean ok = fi.delete();
            if (!ok) {
                fi.deleteOnExit();
            }
        }

        if (res != null) {
            os.println("Deleting target files...");
            for (String file : res) {
                File fi = new File(file);
                fi.delete();
                if (file.endsWith(".koa")) {
                    new File(file+".png").delete();
                }
                fi.getParentFile().delete();
            }
        }

        os.flush();
    }

    private boolean delete(String ilTarget) {
        return !new File(ilTarget).exists() || new File(ilTarget).delete();
    }

    private boolean getBoolean(String parameter, HttpServletRequest request) {
        String val = request.getParameter(parameter);
        if (val != null && val.equals("1")) {
            return true;
        }
        return false;
    }

    private int getNumber(String parameter, HttpServletRequest request) {
        String nums = request.getParameter(parameter);
        if (nums != null) {
            nums = nums.trim();
            if (nums.isEmpty()) {
                return -1;
            }
            try {
                if (nums.startsWith("$")) {
                    nums = nums.substring(1);
                    return Integer.parseInt(nums, 16);
                }
                return Integer.parseInt(nums);
            } catch (Exception e) {
                return -999;
            }
        }
        return -1;
    }

    private Parameters readParameters(HttpServletRequest request) {
        Parameters params = new Parameters();

        params.setPlatform(request.getParameter("platform"));
        params.setBackgroundDefault(getBoolean("backgrounddefault", request));
        params.setExcludeAlpha(getBoolean("alpha", request));
        params.setLowerCase(getBoolean("lower", request));
        params.setBackground(getNumber("background", request));
        params.setColorMapper(getNumber("colormapper", request));
        params.setPrescale(getNumber("prescale", request));
        params.setBoost((float) getNumber("boost", request)/10f);
        params.setKoala(getBoolean("koala", request));
        params.setKoalaDither(getNumber("koaladither", request));

        return params;
    }
}

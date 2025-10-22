package com.quincy.sdk.helper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.quincy.core.InnerConstants;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CommonHelper {
	private static ParamSupport paramSupportHead;
	private static I18NSupport i18nSupportHead;
	public static String[] SUPPORTED_LOCALES;
	private final static String I18N_KEY = "_"+InnerConstants.KEY_LOCALE;

	public static Locale getLocale() {
		HttpServletRequest request = getRequest();
		return getLocale(request);
	}

	public static Locale getLocale(HttpServletRequest request) {
		Locale locale = null;
		Object _locale = request.getAttribute(I18N_KEY);
		if(_locale!=null) {
			locale = (Locale)_locale;
		} else {
			String localeStr = i18nSupportHead.support(request);
			locale = StringUtils.parseLocale(localeStr);
			request.setAttribute(I18N_KEY, locale);
		}
		return locale;
	}

	static {
		ParamSupport headerParamSupport = new ParamSupport() {
			@Override
			protected String resolve(HttpServletRequest request, String key) {
				return request.getHeader(key);
			}
		};
		ParamSupport paramParamSupport = new ParamSupport() {
			@Override
			protected String resolve(HttpServletRequest request, String key) {
				return request.getParameter(key);
			}
		};
		ParamSupport cookieParamSupport = new ParamSupport() {
			@Override
			protected String resolve(HttpServletRequest request, String key) {
				return getValueFromCookie(request, key);
			}
		};
		headerParamSupport.setNext(paramParamSupport).setNext(cookieParamSupport);
		paramSupportHead = headerParamSupport;

		I18NSupport paramI18NSupport = new I18NSupport() {
			@Override
			protected String resolve(HttpServletRequest request) {
				return getValue(request, InnerConstants.KEY_LOCALE);
			}
		};
		I18NSupport uriI18NSupport = new I18NSupport() {
			@Override
			protected String resolve(HttpServletRequest request) {
				return getFirstAsUri(request);
			}
		};
		I18NSupport defaultI18NSupport = new I18NSupport() {
			@Override
			protected String resolve(HttpServletRequest request) {
				return getDefaultLocale(request);
			}
		};
		paramI18NSupport.setNext(uriI18NSupport).setNext(defaultI18NSupport);
		i18nSupportHead = paramI18NSupport;
	}

	private static abstract class ParamSupport {
		private ParamSupport next;

		protected abstract String resolve(HttpServletRequest request, String key);

		public ParamSupport setNext(ParamSupport next) {
			this.next = next;
			return next;
		}

		public String support(HttpServletRequest request, String key) {
			String value = trim(this.resolve(request, key));
			return value!=null?value:(this.next==null?null:this.next.support(request, key));
		}
	}

	private static abstract class I18NSupport {
		private I18NSupport next;

		protected abstract String resolve(HttpServletRequest request);

		public I18NSupport setNext(I18NSupport next) {
			this.next = next;
			return next;
		}

		public String support(HttpServletRequest request) {
			String locale = trim(this.resolve(request));
			if(locale!=null) {
				for(String supportedLocale:SUPPORTED_LOCALES) {
					if(supportedLocale.equalsIgnoreCase(locale))
						return locale;
				}
			}
			return this.next==null?"zh_CN":this.next.support(request);
		}
	}

	public static String getDefaultLocale(HttpServletRequest request) {
		Locale locale = request.getLocale();
		return locale.getLanguage()+"_"+locale.getCountry();
	}

	public static String trim(String s) {
		if(s!=null) {
			String _s = s.trim();
			if(_s.length()>0)
				return _s;
		}
		return null;
	}

	public static HttpServletRequest getRequest() {
		return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
	}

	public static HttpServletResponse getResponse() {
		return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
	}

	public static String getValueFromCookie(HttpServletRequest request, String key) {
		Cookie[] cookies = request.getCookies();
		if(cookies!=null&&cookies.length>0) {
			for(Cookie cookie:cookies) {
				if(cookie.getName().equals(key))
					return cookie.getValue();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String getValue(HttpServletRequest request, String key) {
		String value = null;
		Map<String, String> map = null;
		Object _map = request.getAttribute("requestParams");
		if(_map==null) {
			map = new HashMap<String, String>();
			request.setAttribute("requestParams", map);
		} else {
			map = (Map<String, String>)_map;
			value = map.get(key);
		}
		if(value==null) {
			value = paramSupportHead.support(request, key);
			if(value==null)
				value = "";
			map.put(key, value);
		}
		return value.length()==0?null:value;
	}

	public static String getFirstAsUri(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String[] ss = uri.split("/");
		return ss.length<2?null:ss[1];
	}

	public static byte[] input2bytes(InputStream in) throws IOException {
        byte[] buff = new byte[100];
        int rc = 0;
        ByteArrayOutputStream out = null;
        try {
        	out = new ByteArrayOutputStream();
            while((rc = in.read(buff, 0, 100))>0)
            	out.write(buff, 0, rc);
            byte[] b = out.toByteArray();
            return b;
        } finally {
        	if(out!=null)
        		out.close();
        }
    }

	public static String input2String(InputStream in) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new BufferedInputStream(in)));
			StringBuilder result = new StringBuilder();
			String line = null;
			while((line = br.readLine())!=null)
				result.append(line);
			return result.toString();
		} finally {
			if(br!=null)
				br.close();
		}
    }

	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;
		try {
			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
		} finally {
			if(bos!=null)
				bos.close();
			if(oos!=null)
				oos.close();
		}
	}

	public static Object unSerialize(byte[] byteArray) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
        	bis = new ByteArrayInputStream(byteArray);
			ois = new ObjectInputStream(bis);
			return ois.readObject();
		} finally {
			if(bis!=null)
				bis.close();
			if(ois!=null)
				ois.close();
		}
	}

	public static void deleteFileOrDir(File file) {
		if(file.isDirectory()) {
			File[] files = file.listFiles();
			for(File subFile:files)
				deleteFileOrDir(subFile);
		}
		file.delete();
	}

	public static void deleteFileR(File dir, String name) {
		if(dir.isDirectory()) {
			File[] files = dir.listFiles();
			for(File subFile:files) {
				if(subFile.isDirectory()) {
					deleteFileR(subFile, name);
				} else if(subFile.getName().equals(name))
					subFile.delete();
			}
		}
	}

	public static void deleteFileR(String dir, String name) {
		deleteFileR(new File(dir), name);
	}

	public static void unzip(InputStream in, Charset charset, int buffer, String _dst) throws IOException {
		String dst = _dst.endsWith("/")?_dst:_dst+"/";
		ZipInputStream zipIn = null;
		OutputStream out = null;
		try {
			zipIn = new ZipInputStream(in, charset);
			java.util.zip.ZipEntry entry = null;
			while((entry=zipIn.getNextEntry())!=null) {
				if(entry.isDirectory()) {
					File dir = new File(dst+entry.getName());
					if(!dir.exists())
						dir.mkdirs();
				} else {
					String relativePath = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
					String absolutePath = dst+"/"+relativePath;
					File dir = new File(absolutePath);
					if(!dir.exists())
						dir.mkdirs();
					File file = new File(dst+entry.getName());
					if(file.exists())
						file.delete();
					int n;
					byte[] b = new byte[buffer];
					out = new FileOutputStream(dst+entry.getName());
					while((n=zipIn.read(b, 0, b.length))!=-1)
						out.write(b, 0, n);
					out.flush();
					out.close();
				}
			}
		} finally {
			if(out!=null)
				out.close();
			if(zipIn!=null)
				zipIn.close();
		}
	}

	public static void unzip(String src, String _dst, int buffer) throws IOException {
		unzip(new File(src), _dst, buffer);
	}

	public static void unzip(File src, String _dst, int buffer) throws IOException {
		String dst = _dst.endsWith("/")?_dst:_dst+"/";
		ZipFile zipFile = null;
		OutputStream out = null;
		InputStream in = null;
		try {
			zipFile = new ZipFile(src);
			Enumeration<ZipEntry> e = zipFile.getEntries();
			while(e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				if(entry.isDirectory()) {
					File dir = new File(dst+entry.getName());
					if(!dir.exists())
						dir.mkdirs();
				} else {
					String relativePath = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
					String absolutePath = dst+"/"+relativePath;
					File dir = new File(absolutePath);
					if(!dir.exists())
						dir.mkdirs();
					File file = new File(dst+entry.getName());
					if(file.exists())
						file.delete();
					int n;
					byte[] b = new byte[buffer];
					out = new FileOutputStream(file);
					in = zipFile.getInputStream(entry);
					while((n=in.read(b, 0, b.length))!=-1)
						out.write(b, 0, n);
					out.flush();
					out.close();
					in.close();
				}
			}
		} finally {
			if(out!=null)
				out.close();
			if(in!=null)
				in.close();
			if(zipFile!=null)
				zipFile.close();
		}
	}

	public static File zip(String[] src, String dst) throws IOException {
		File dstFile = new File(dst);
		if(dstFile.exists())
			dstFile.delete();
		ZipOutputStream zipOut = null;
		try {
			zipOut = new ZipOutputStream(new FileOutputStream(dstFile));
			zipOut.setEncoding("UTF-8");
			for(String s:src)
				appendZipEntry(zipOut, new File(s), "");
			zipOut.flush();
			return dstFile;
		} finally {
			if(zipOut!=null)
				zipOut.close();
		}
	}

	private static void appendZipEntry(ZipOutputStream zipOut, File srcFile, String folder) throws IOException {
		String path = folder+srcFile.getName();
		ZipEntry zipEntry = new ZipEntry(path);
		zipEntry.setUnixMode(755);
		if(srcFile.isFile()) {
			zipEntry.setSize(srcFile.length());
			zipEntry.setTime(srcFile.lastModified());
            InputStream in = null;
            try {
            		in = new BufferedInputStream(new FileInputStream(srcFile));
            		byte[] b = new byte[in.available()];
            		in.read(b);
            		zipOut.putNextEntry(zipEntry);
            		zipOut.write(b);
            } finally {
            		if(in!=null)
            			in.close();
            }
		} else {
			File[] files = srcFile.listFiles();
			for(File file:files)
				appendZipEntry(zipOut, file, folder+srcFile.getName()+"/");
		}
	}

	public static String chineseToUnicode(String arg) {  
        StringBuilder sb = new StringBuilder(arg.length()*5);
        char[] cArray = arg.toCharArray();
        for(char c:cArray) {
        	if(c>=19968&&c<=171941) {//汉字范围 \u4e00-\u9fa5 (中文)
        		sb.append("\\u").append(Integer.toHexString(c));
        	} else
        		sb.append(c);
        }
        return sb.toString();
    }

	public static String fullMethodPath(Class<?> clazz, MethodSignature methodSignature, Method method, Object[] args, String _separator0, String _separator1, String _separator2) throws NoSuchMethodException, SecurityException {
		StringBuilder sb = new StringBuilder(200).append(clazz.getName()).append(trim(_separator0)).append(methodSignature.getName());
		String separator1 = trim(_separator1);
		String separator2 = trim(_separator2);
		if(separator1!=null&&separator2!=null) {
			if(method==null)
				method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
			Class<?>[] clazzes = method.getParameterTypes();
			if(args!=null&&args.length>0) {
				for(int i=0;i<args.length;i++) {
					Object arg = args[i];
					sb.append(separator1).append(clazzes[i].getName()).append(separator2).append(arg==null?"null":arg.toString().trim());
				}
			}
		}
		return sb.toString();
	}

	public static String fullMethodPath(ProceedingJoinPoint joinPoint, String separator0, String separator1, String separator2) throws NoSuchMethodException, SecurityException {
		Class<?> clazz = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		String key = fullMethodPath(clazz, methodSignature, method, joinPoint.getArgs(), separator0, separator1, separator2);
		return key;
	}

	public static boolean isEmail(String _content) {
		String content = trim(_content);
		return content==null?false:Pattern.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", content);
	}

	public static boolean isMobilePhone(String _content) {
		String content = trim(_content);
		return content==null?false:Pattern.matches("^1[3456789]\\d{9}$", content);
	}

	public static StringBuilder appendUriParam(StringBuilder s, String key, String val) {
		return val==null?s:s.append(s.indexOf("?")<0?"?":"&")
				.append(key)
				.append("=")
				.append(val);
	}

	public static boolean instanceofX(Object o, Class<?> clazz) {
    	return instanceofX(o.getClass(), clazz);
    }

    public static boolean instanceofX(Class<?> src, Class<?> dst) {
    	if(src==null||dst==null) {
    		return false;
    	} else {
    		String srcClassName = src.getName();
    		if(srcClassName.equals(dst.getName())) {
    			return true;
    		} else {
    			Class<?> superclass = src.getSuperclass();
    			boolean instanceofX = superclass==null?false:instanceofX(src.getSuperclass(), dst);
    			if(instanceofX) {
    				return true;
    			} else {
    				Class<?>[] interfaces = src.getInterfaces();
    				if(interfaces==null||interfaces.length==0) {
    					return false;
    				} else {
    					for(Class<?> i:interfaces) {
    						if(instanceofX(i, dst))
    							return true;
    					}
    					return false;
    				}
    			}
    		}
    	}
    }

    private final static Map<Character, Character> HEAD_MAP = new HashMap<Character, Character>(4);
	private final static Map<Character, Character> TAIL_MAP = new HashMap<Character, Character>(4);
	static {
		HEAD_MAP.put('{', '}');
		HEAD_MAP.put('[', ']');
		HEAD_MAP.put('(', ')');
		HEAD_MAP.put('<', '>');
		TAIL_MAP.put('}', '{');
		TAIL_MAP.put(']', '[');
		TAIL_MAP.put(')', '(');
		TAIL_MAP.put('>', '<');
	}

	public static boolean formatTest(String s) {
		char[] cc = s.toCharArray();
		if(HEAD_MAP.get(cc[0])==null||TAIL_MAP.get(cc[cc.length-1])==null)
			return false;
		Stack<Character> stack = new Stack<Character>();
		for(char c:cc) {
			if(HEAD_MAP.get(c)!=null) {
				stack.push(c);
			} else {
				Character h = TAIL_MAP.get(c);
				if(h!=null) {
					if(stack.isEmpty())
						return false;
					char p = stack.pop();
					if(h!=p)
						return false;
				}
			}
		}
		return stack.size()==0;
	}

	public static int maxCommonDivisor(int... args) {
		Integer minValue = null;
		int minIndex = -1;
		for(int i=0;i<args.length;i++) {
			int a = args[i];
			a = a<0?a*-1:a;
			if(minValue==null||a<minValue) {
				minValue = a;
				minIndex = i;
			}
		}
		for(int i=1;i<=minValue/2;i++) {
			if(minValue%i==0) {
				int divisor = minValue/i;
				boolean isMaxCommon = true;
				for(int j=0;j<args.length;j++) {
					if(j!=minIndex) {
						if(args[j]%divisor!=0) {
							isMaxCommon = false;
							break;
						}
					}
				}
				if(isMaxCommon)
					return divisor;
			}
		}
		return 1;
	}

	public static int minCommonMultiple(int... args) {
		Integer maxValue = null;
		int maxIndex = -1;
		int exceptMaxMultiple = 1;
		for(int i=0;i<args.length;i++) {
			int a = args[i];
			a = a<0?a*-1:a;
			if(maxValue==null||a>maxValue) {
				maxValue = a;
				maxIndex = i;
			}
			exceptMaxMultiple *= a;
		}
		exceptMaxMultiple /= maxValue;
		for(int i=1;i<exceptMaxMultiple;i++) {
			int multiple = maxValue*i;
			boolean isMinCommon = true;
			for(int j=0;j<args.length;j++) {
				if(j!=maxIndex) {
					if(multiple%args[j]!=0) {
						isMinCommon = false;
						break;
					}
				}
			}
			if(isMinCommon)
				return multiple;
		}
		return exceptMaxMultiple*maxValue;
	}

	public static void main(String[] args) throws IOException {
//		zip(new String[] {"D:/fxcupload/quincy"}, "D:/fxcupload/quincy.zip");
//		CommonHelper.unzip("D:/fxcupload/quincy.zip", "D:/fxcupload/xxx", 2*1024*1024);
		/*Charset gbk = Charset.forName("GBK");
		InputStream in = null;
		try {
			in = new FileInputStream("D:/fxcupload/quincy.zip");
			CommonHelper.unzip(in, gbk, 2*1024*1024, "D:/fxcupload/xxx");
		} finally {
			if(in!=null)
				in.close();
		}*/
		deleteFileR("/Users/maqiang/github/yy", ".DS_Store");
	}
}
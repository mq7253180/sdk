package com.quincy.sdk.helper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class AopHelper {
	public static Method getMethod(JoinPoint joinPoint) throws NoSuchMethodException, SecurityException {
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Class<?> clazz = joinPoint.getTarget().getClass();
        return clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
	}

	public static <T extends Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> annotationClass) throws NoSuchMethodException, SecurityException {
        return getMethod(joinPoint).getAnnotation(annotationClass);
	}

	public static String extractBeanName(Class<?> clazz) {
		String beanName = CommonHelper.trim(chainHead.support(clazz));
		if(beanName==null) {
			String simpleClassName = clazz.getSimpleName();
			String firstCharLowerCase = simpleClassName.substring(0, 1).toLowerCase();
			if(simpleClassName.length()==1) {
				beanName = firstCharLowerCase;
			} else {
				char secondChar = simpleClassName.toCharArray()[1];
				int secondCharAscii = (int)secondChar;
				//如果第二个字母是大写，beanName就是原类名
				beanName = (secondCharAscii>=65&&secondCharAscii<=90)?simpleClassName:(firstCharLowerCase+simpleClassName.substring(1, simpleClassName.length()));
			}
		}
		return beanName;
	}

	public static boolean isControllerMethod(JoinPoint joinPoint) throws NoSuchMethodException, SecurityException {
    	Method method = AopHelper.getMethod(joinPoint);
    	boolean isController = mappingSupportHead.support(method);
    	return isController;
    }

	private static Support chainHead;
	private static MappingSupport mappingSupportHead;

	static {
		Support serviceSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Service annotation = clazz.getDeclaredAnnotation(Service.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support componentSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Component annotation = clazz.getDeclaredAnnotation(Component.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support controllerSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Controller annotation = clazz.getDeclaredAnnotation(Controller.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support repositorySupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Repository annotation = clazz.getDeclaredAnnotation(Repository.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support configurationSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Configuration annotation = clazz.getDeclaredAnnotation(Configuration.class);
				return annotation==null?null:annotation.value();
			}
		};
		serviceSupport.setNext(componentSupport).setNext(controllerSupport).setNext(repositorySupport).setNext(configurationSupport);
		chainHead = serviceSupport;

		MappingSupport requestMappingSupport = new MappingSupport() {
			@Override
			protected boolean resolve(Method method) {
				return method.getAnnotation(RequestMapping.class)!=null;
			}
		};
		MappingSupport postMappingSupport = new MappingSupport() {
			@Override
			protected boolean resolve(Method method) {
				return method.getAnnotation(PostMapping.class)!=null;
			}
		};
		MappingSupport getMappingSupport = new MappingSupport() {
			@Override
			protected boolean resolve(Method method) {
				return method.getAnnotation(GetMapping.class)!=null;
			}
		};
		MappingSupport putMappingSupport = new MappingSupport() {
			@Override
			protected boolean resolve(Method method) {
				return method.getAnnotation(PutMapping.class)!=null;
			}
		};
		MappingSupport deleteMappingSupport = new MappingSupport() {
			@Override
			protected boolean resolve(Method method) {
				return method.getAnnotation(DeleteMapping.class)!=null;
			}
		};
		requestMappingSupport.setNext(postMappingSupport).setNext(getMappingSupport).setNext(putMappingSupport).setNext(deleteMappingSupport);
		mappingSupportHead = requestMappingSupport;
	}

	private static abstract class Support {
		private Support next;

		protected abstract String resolve(Class<?> clazz);

		public Support setNext(Support next) {
			this.next = next;
			return next;
		}

		public String support(Class<?> clazz) {
			String beanName = this.resolve(clazz);
			return beanName==null?(this.next==null?null:this.next.support(clazz)):beanName;
		}
	}

	private static abstract class MappingSupport {
		private MappingSupport next;

		protected abstract boolean resolve(Method method);

		public MappingSupport setNext(MappingSupport next) {
			this.next = next;
			return next;
		}

		public boolean support(Method method) {
			return this.resolve(method)?true:(this.next==null?false:this.next.support(method));
		}
	}
}
package com.quincy.auth.service.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.quincy.auth.entity.Menu;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.mapper.AuthMapper;
import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthorizationService;

public abstract class AuthorizationAbstract implements AuthorizationService {
	protected abstract Object getUserObject(HttpServletRequest request) throws Exception;
	protected abstract void saveVcode(HttpServletRequest request, String vcode) throws Exception;

	public DSession getSession(HttpServletRequest request) throws Exception {
		Object obj = this.getUserObject(request);
		return obj==null?null:(DSession)obj;
	}

	private final static int width = 88;
	private final static int height = 40;
	private final static int lines = 5;
	
	private final static String VCODE_COMBINATION_FROM = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";

	public void vcode(HttpServletRequest request, HttpServletResponse response, int length) throws Exception {
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		for(int i=0;i<length;i++)
			sb.append(VCODE_COMBINATION_FROM.charAt(random.nextInt(VCODE_COMBINATION_FROM.length())));
		String vcode = sb.toString();
		this.saveVcode(request, vcode);
		OutputStream out = null;
		try {
			out = response.getOutputStream();
			this.drawAsByteArray(vcode, random, out);
			out.flush();
		} finally {
			if(out!=null)
				out.close();
		}
	}

	private void drawAsByteArray(String arg, Random random, OutputStream output) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		Graphics g = image.getGraphics();
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
		for(int i=0;i<lines;i++)
			g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
		Font font = new Font("Times New Roman", Font.ROMAN_BASELINE, 25);
		g.setFont(font);
		g.setColor(new Color(random.nextInt(101), random.nextInt(111), random.nextInt(121)));
//		g.translate(random.nextInt(3), random.nextInt(3));
		g.drawString(arg, 13, 25);
		ImageIO.write(image, "jpg", output);
	}

	@Autowired
	private AuthMapper authMapper;

	protected DSession createSession(Long userId) {
		DSession session = new DSession();
		//角色
		List<Role> roleList = authMapper.findRolesByUserId(userId);
		Map<Long, String> roleMap = new HashMap<Long, String>(roleList.size());
		for(Role role:roleList)//去重
			roleMap.put(role.getId(), role.getName());
		List<String> roles = new ArrayList<String>(roleMap.size());
		roles.addAll(roleMap.values());
		session.setRoles(roles);
		//权限
		List<Permission> permissionList = authMapper.findPermissionsByUserId(userId);
		Map<Long, String> permissionMap = new HashMap<Long, String>(permissionList.size());
		for(Permission permission:permissionList)//去重
			permissionMap.put(permission.getId(), permission.getName());
		List<String> permissions = new ArrayList<String>(permissionMap.size());
		permissions.addAll(permissionMap.values());
		session.setPermissions(permissions);
		//菜单
		List<Menu> rootMenus = this.findMenusByUserId(userId);
		session.setMenus(rootMenus);
		return session;
	}

	protected DSession createSession(User user) {
		DSession session = this.createSession(user.getId());
		session.setUser(user);
		return session;
	}

	private List<Menu> findMenusByUserId(Long userId) {
		List<Menu> allMenus = authMapper.findMenusByUserId(userId);
		Map<Long, Menu> duplicateRemovedMenus = new HashMap<Long, Menu>(allMenus.size());
		for(Menu menu:allMenus)
			duplicateRemovedMenus.put(menu.getId(), menu);
		List<Menu> rootMenus = new ArrayList<Menu>(duplicateRemovedMenus.size());
		Set<Entry<Long, Menu>> entrySet = duplicateRemovedMenus.entrySet();
		for(Entry<Long, Menu> entry:entrySet) {
			Menu menu = entry.getValue();
			if(menu.getPId()==null) {
				rootMenus.add(menu);
				this.loadChildrenMenus(menu, entrySet);
			}
		}
		return rootMenus;
	}

	private void loadChildrenMenus(Menu parent, Set<Entry<Long, Menu>> entrySet) {
		for(Entry<Long, Menu> entry:entrySet) {
			Menu menu = entry.getValue();
			if(parent.getId()==menu.getPId()) {
				if(parent.getChildren()==null)
					parent.setChildren(new ArrayList<Menu>(10));
				parent.getChildren().add(menu);
			}
		}
		if(parent.getChildren()!=null&&parent.getChildren().size()>0) {
			for(Menu child:parent.getChildren())
				this.loadChildrenMenus(child, entrySet);
		}
	}
}
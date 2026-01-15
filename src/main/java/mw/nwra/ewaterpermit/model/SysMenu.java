package mw.nwra.ewaterpermit.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_menu")
@NamedQuery(name = "SysMenu.findAll", query = "SELECT s FROM SysMenu s")
public class SysMenu extends BaseEntity {

	private String description;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "menu_url")
	private String menuUrl;

	@Column(name = "order_index")
	private int orderIndex;

	private byte status;

	// bi-directional many-to-one association to SysMenu
	@ManyToOne
	@JoinColumn(name = "parent_menu_id")
	private SysMenu sysMenu;

	// bi-directional many-to-one association to SysMenu
	@OneToMany(mappedBy = "sysMenu")
	private List<SysMenu> sysMenus;

	// bi-directional many-to-one association to SysObject
	@ManyToOne
	@JoinColumn(name = "object_id")
	private SysObject sysObject;

	public SysMenu() {
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImageUrl() {
		return this.imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getMenuUrl() {
		return this.menuUrl;
	}

	public void setMenuUrl(String menuUrl) {
		this.menuUrl = menuUrl;
	}

	public int getOrderIndex() {
		return this.orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public byte getStatus() {
		return this.status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public SysMenu getSysMenu() {
		return this.sysMenu;
	}

	public void setSysMenu(SysMenu sysMenu) {
		this.sysMenu = sysMenu;
	}

	public List<SysMenu> getSysMenus() {
		return this.sysMenus;
	}

	public void setSysMenus(List<SysMenu> sysMenus) {
		this.sysMenus = sysMenus;
	}

	public SysMenu addSysMenus(SysMenu sysMenus) {
		getSysMenus().add(sysMenus);
		sysMenus.setSysMenu(this);

		return sysMenus;
	}

	public SysMenu removeSysMenus(SysMenu sysMenus) {
		getSysMenus().remove(sysMenus);
		sysMenus.setSysMenu(null);

		return sysMenus;
	}

	public SysObject getSysObject() {
		return this.sysObject;
	}

	public void setSysObject(SysObject sysObject) {
		this.sysObject = sysObject;
	}

}
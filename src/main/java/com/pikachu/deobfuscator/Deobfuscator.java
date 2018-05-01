package com.pikachu.deobfuscator;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class Deobfuscator extends JavaPlugin {

	private static SkriptAddon addonInstance;
	private static Deobfuscator instance;

	public static SkriptAddon getAddonInstance() {
		if (addonInstance == null) {
			addonInstance = Skript.registerAddon(getInstance());
		}
		return addonInstance;
	}

	public static Deobfuscator getInstance() {
		if (instance == null) {
			instance = new Deobfuscator();
		}
		return instance;
	}

	@Override
	public void onEnable() {
		instance = this;
		try {
			getAddonInstance().loadClasses("com.pikachu.deobfuscator", "skript");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

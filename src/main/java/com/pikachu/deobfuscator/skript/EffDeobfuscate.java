package com.pikachu.deobfuscator.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.event.Event;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class EffDeobfuscate extends Effect {

	private static final Method NODE_INDENTATION;
	private static final Field CURRENT_OPTIONS;

	static {
		Skript.registerEffect(EffDeobfuscate.class, "deobfuscate [(this|the)] script");

		Method _NODE_INDENTATION = null;
		try {
			_NODE_INDENTATION = Node.class.getDeclaredMethod("getIndentation", null);
			_NODE_INDENTATION.setAccessible(true);
		} catch (NoSuchMethodException e) {
			Skript.error("I was unable to find the indentation method, deobfuscation won't work!");
		}
		NODE_INDENTATION = _NODE_INDENTATION;

		Field _FIELD_MODIFIERS = null;
		try {
			_FIELD_MODIFIERS = Field.class.getDeclaredField("modifiers");
			_FIELD_MODIFIERS.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			Skript.error("Can't reset options -- deobfuscation will still work, but clean up will not.");
		}

		Field _CURRENT_OPTIONS = null;
		try {
			_CURRENT_OPTIONS = ScriptLoader.class.getDeclaredField("currentOptions");
			_CURRENT_OPTIONS.setAccessible(true);
			_FIELD_MODIFIERS.setInt(_CURRENT_OPTIONS, _CURRENT_OPTIONS.getModifiers() & ~Modifier.FINAL);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			Skript.error("I was unable to set up the options field completely, deobfuscation may not work!");
		}
		CURRENT_OPTIONS = _CURRENT_OPTIONS;

	}

	private Config script;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (ScriptLoader.currentScript == null) {
			Skript.error("No script is currently loading!");
			return false;
		}
		script = ScriptLoader.currentScript;
		return NODE_INDENTATION != null && CURRENT_OPTIONS != null;
	}

	public String nodeToString(SectionNode sectionNode) {
		try {
			StringBuilder builder = new StringBuilder();
			if (((String) NODE_INDENTATION.invoke(sectionNode, null)).isEmpty()) {
				builder.append(sectionNode.getKey());
				builder.append(":");
			}
			for (Node node : sectionNode) {
				String indentation = (String) NODE_INDENTATION.invoke(node, null);
				builder.append("\n");
				builder.append(indentation);
				builder.append(ScriptLoader.replaceOptions(node.getKey()));
				if (node instanceof SectionNode) {
					builder.append(":");
					builder.append(nodeToString((SectionNode) node));
				}
			}
			return builder.toString();
		} catch (IllegalAccessException | InvocationTargetException e) {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void execute(Event e) {
		StringBuilder originalScript = new StringBuilder();
		try {
			HashMap<String, String> options = (HashMap<String, String>) CURRENT_OPTIONS.get(null);
			options.clear();
			HashMap<String, String> optionsCopy = new HashMap<>(options);
			for (Node n : script.getMainNode()) {
				if (n instanceof SectionNode) {
					SectionNode node = (SectionNode) n;
					if ("options".equalsIgnoreCase(n.getKey())) { // hard to believe skript doesn't have a method for this, its just hardcoded
						node.convertToEntries(0);
						for (Node option : node) {
							if (!(option instanceof EntryNode)) {
								Skript.error("invalid line in options");
								continue;
							}
							options.put(option.getKey(), ((EntryNode) option).getValue());
						}
					} else {
						originalScript.append("\n\n");
						originalScript.append(nodeToString(node));
					}
				}
			}
			CURRENT_OPTIONS.set(null, optionsCopy);
		} catch (IllegalAccessException e1) {
			Skript.error("Failed to manipulate Skript's options field!");
			e1.printStackTrace();
		}
		String deobfuscated = originalScript.substring(2);
		File location = script.getFile() == null ?
				new File("plugins/Skript/scripts/debofuscated.sk")
				: new File("plugins/Skript/scripts/debofuscated_"
				+ FilenameUtils.getBaseName(script.getFile().getName()) + ".sk");
		try {
			FileUtils.write(location, deobfuscated, "UTF-8");
		} catch (IOException e1) {
			Skript.error("Failed to save deobfuscated script!");
			e1.printStackTrace();
		}
	}

	@Override
	public String toString(Event e, boolean debug) {
		return "deobfuscate script";
	}

}

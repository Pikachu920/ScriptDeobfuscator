package com.pikachu.deobfuscator.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EffDeobfuscate extends Effect {

	private static final Method NODE_INDENTATION;

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
	}

	private Config script;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (ScriptLoader.currentScript == null) {
			Skript.error("No script is currently loading!");
			return false;
		}
		script = ScriptLoader.currentScript;
		return NODE_INDENTATION != null;
	}

	public String nodeToString(SectionNode sectionNode) {
		try {
			StringBuilder builder = new StringBuilder();
			for (Node node : sectionNode) {
				String indentation = (String) NODE_INDENTATION.invoke(node, null);
				if (!("options".equals(node.getKey()) && indentation.isEmpty())) { // options section isn't useful after this
					builder.append("\n");
					builder.append(indentation);
					if (indentation.isEmpty()) { // if the indentation is empty, it's an event and we should space it
						builder.append("\n");
					}
					builder.append(ScriptLoader.replaceOptions(node.getKey()));
					if (node instanceof SectionNode) {
						builder.append(":");
						builder.append(nodeToString((SectionNode) node));
					}
				}
			}
			return builder.toString();
		} catch (IllegalAccessException | InvocationTargetException e) {
			return null;
		}
	}

	@Override
	protected void execute(Event e) {
		String deobfuscated = nodeToString(script.getMainNode()).substring(2);
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

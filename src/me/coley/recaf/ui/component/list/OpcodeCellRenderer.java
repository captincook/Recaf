package me.coley.recaf.ui.component.list;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Options;
import me.coley.recaf.asm.Access;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.FontUtil;

public class OpcodeCellRenderer implements ListCellRenderer<AbstractInsnNode>, Opcodes {
	private final MethodNode method;
	private final Options options;
	private final String colBlueDark = "#193049";
	private final String colTealDark = "#154234";
	private final String colGreenDark = "#184216";
	private final String colRedDark = "#351717";
	private final String colGray = "#555555";

	public OpcodeCellRenderer(MethodNode method, Options options) {
		this.method = method;
		this.options = options;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends AbstractInsnNode> list, AbstractInsnNode value, int index,
			boolean isSelected, boolean cellHasFocus) {
		OpcodeList opcodeCastedList = (OpcodeList) list;
		JLabel label = new JLabel(getOpcodeText(value));
		label.setFont(FontUtil.monospace);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createEtchedBorder());
		if (isSelected) {
			label.setBackground(Color.white);
		} else {
			label.setBackground(opcodeCastedList.getColorFor(index, value));
		}
		return label;
	}

	public String getOpcodeText(AbstractInsnNode ain) {
		int ainIndex = method.instructions.indexOf(ain);
		int zeros = String.valueOf(method.instructions.size()).length() - String.valueOf(ainIndex).length() + 1;
		String ss = "";
		for (int i = 0; i < zeros; i++) {
			ss += "&nbsp;";
		}
		String s = "<html>" + color(colGray, ainIndex + ".") + ss + "<b>" + OpcodeUtil.opcodeToName(ain.getOpcode()) + "</b>";
		switch (ain.getType()) {
		case AbstractInsnNode.INT_INSN:
			// Add int value to string
			IntInsnNode insnInt = (IntInsnNode) ain;
			s += " " + insnInt.operand;
			break;
		case AbstractInsnNode.VAR_INSN:
			// Add local variable index to string
			VarInsnNode insnVar = (VarInsnNode) ain;
			s += " " + insnVar.var;
			// Add local variable name if possible
			if (insnVar.var < method.localVariables.size()) {
				LocalVariableNode var = method.localVariables.get(insnVar.var);
				String varStr = var.name;
				s += color(colBlueDark, italic(" (" + varStr + ")"));
			} else if (insnVar.var == 0 && !Access.isStatic(method.access)) {
				// If the local variable doesn't have a name, we can assume at
				// index = 0 that it is 'this'.
				s += color(colBlueDark, italic(" (this)"));
			}
			break;
		case AbstractInsnNode.TYPE_INSN:
			// Add type name to string
			TypeInsnNode insnType = (TypeInsnNode) ain;
			String typeDeclaredStr = getTypeStr(Type.getType(insnType.desc));
			s += color(colBlueDark, italic(" (" + typeDeclaredStr + ")"));
			break;
		case AbstractInsnNode.FIELD_INSN:
			FieldInsnNode insnField = (FieldInsnNode) ain;
			s += " " + italic(color(colBlueDark, getTypeStr(Type.getType(insnField.desc)))) + " ";
			s += color(colRedDark, getTypeStr(Type.getObjectType(insnField.owner))) + "." + escape(insnField.name);
			break;
		case AbstractInsnNode.METHOD_INSN:
			MethodInsnNode insnMethod = (MethodInsnNode) ain;
			Type typeMethod = Type.getMethodType(insnMethod.desc);
			// Args string
			String args = "";
			for (Type t : typeMethod.getArgumentTypes()) {
				args += getTypeStr(t) + ", ";
			}
			if (args.endsWith(", ")) {
				args = args.substring(0, args.length() - 2);
			}
			String retType = getTypeStr(typeMethod.getReturnType());
			s += " " + italic(color(colBlueDark, retType)) + " ";
			s += color(colRedDark, getTypeStr(Type.getObjectType(insnMethod.owner))) + "." + escape(insnMethod.name) + "(";
			s += color(colTealDark, args);
			s += ")";
			break;
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			break;
		case AbstractInsnNode.JUMP_INSN:
			JumpInsnNode insnJump = (JumpInsnNode) ain;
			if (insnJump.label != null) {
				s += " " + method.instructions.indexOf(insnJump.label);
			}
			if (options.opcodeShowJumpHelp) {
				//@formatter:off
				String z = "";
				switch (ain.getOpcode()) {
				case IFEQ     : z = "($ == 0 -> offset)";   break;
				case IFNE     : z = "($ != 0 -> offset)";   break;
				case IFLE     : z = "($ <= 0 -> offset)";   break;
				case IFLT     : z = "($ < 0 -> offset)";    break;
				case IFGE     : z = "($ >= 0 -> offset)";   break;
				case IFGT     : z = "($ > 0 -> offset)";    break;
				case IF_ACMPNE: z = "($1 != $2 -> offset)"; break;
				case IF_ACMPEQ: z = "($1 == $2 -> offset)"; break;
				case IF_ICMPEQ: z = "($1 == $2 -> offset)"; break;
				case IF_ICMPNE: z = "($1 != $2 -> offset)"; break;
				case IF_ICMPLE: z = "($1 <= $2 -> offset)"; break;
				case IF_ICMPLT: z = "($1 < $2 -> offset)";  break;
				case IF_ICMPGE: z = "($1 >= $2 -> offset)"; break;
				case IF_ICMPGT: z = "($1 > $2 -> offset)";  break;
				case GOTO     : z = "(-> offset)";          break;
				case JSR      : z = "(-> offset, +address)";break;
				case IFNULL   : z = "($ == null -> offset)";break;
				case IFNONNULL: z = "($ != null -> offset)";break;
				}
				//@formatter:on
				s += " " + italic(color(colGray, z));
			}
			break;
		case AbstractInsnNode.LDC_INSN:
			LdcInsnNode insnLdc = (LdcInsnNode) ain;
			String x = italic(color(colGreenDark, insnLdc.cst.toString()));
			if (insnLdc.cst instanceof String) {
				x = "\"" + x + "\"";
			}
			s += " " + x;
			break;
		case AbstractInsnNode.IINC_INSN:
			// Add local variable index to string
			IincInsnNode insnIinc = (IincInsnNode) ain;
			s += " " + insnIinc.var;
			// Add local variable name if possible
			if (insnIinc.var < method.localVariables.size()) {
				LocalVariableNode var = method.localVariables.get(insnIinc.var);
				String varStr = var.name;
				s += color(colBlueDark, italic(" (" + varStr + ") "));
			} else if (insnIinc.var == 0 && !Access.isStatic(method.access)) {
				// If the local variable doesn't have a name, we can assume at
				// index = 0 that it is 'this'.
				s += color(colBlueDark, italic(" (this) "));
			}
			if (insnIinc.incr > 0) {
				s += color(colRedDark, "+" + insnIinc.incr);
			} else {
				s += color(colRedDark, "-" + insnIinc.incr);
			}
			break;
		case AbstractInsnNode.TABLESWITCH_INSN:
			TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
			int tableDefaultOffset = method.instructions.indexOf(insnTableSwitch.dflt);

			s += " " + color(colGray, "range:[" + insnTableSwitch.min + "-" + insnTableSwitch.max + "] default:"
					+ tableDefaultOffset);
			// TODO
			break;
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) ain;
			String u = "";
			for (int i = 0; i < insnLookupSwitch.keys.size(); i++) {
				int offset = method.instructions.indexOf(insnLookupSwitch.labels.get(i));
				u += insnLookupSwitch.keys.get(i) + ":" + offset + ", ";
			}
			if (insnLookupSwitch.dflt != null) {
				int offset = method.instructions.indexOf(insnLookupSwitch.dflt);
				u += "default:" + offset;
			}
			if (u.endsWith(", ")) {
				u = u.substring(0, u.length() - 2);
			}
			s += color(colGray, italic(" (" + u + ")"));
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			// MultiANewArrayInsnNode insnArray = (MultiANewArrayInsnNode) ain;
			// TODO
			break;
		case AbstractInsnNode.FRAME:
			// TODO
			break;
		case AbstractInsnNode.LINE:
			LineNumberNode line = (LineNumberNode) ain;
			if (options.opcodeSimplifyDescriptors) {
				s = s.replace("F_NEW", "");
			} else {
				s += " ";
			}
			s += color(colGreenDark, italic("line #" + line.line));
			break;

		}
		return s + "</html>";
	}

	/**
	 * HTML escape '&', '<' and '>'.
	 * 
	 * @param s
	 *            Text to escape
	 * @return
	 */
	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Converts a given type to a string. Output will be simplified if enabled
	 * in {@link #options}.
	 * 
	 * @param type
	 * @return
	 */
	private String getTypeStr(Type type) {
		String s = "";
		if (type.getDescriptor().length() == 1) {
			switch (type.getDescriptor().charAt(0)) {
			case 'Z':
				return "boolean";
			case 'I':
				return "int";
			case 'J':
				return "long";
			case 'D':
				return "double";
			case 'F':
				return "float";
			case 'B':
				return "byte";
			case 'C':
				return "char";
			case 'S':
				return "short";
			case 'V':
				return "void";
			default:
				return type.getDescriptor();
			}
		} else {
			s += type.getInternalName();
		}
		// TODO: Make this optional
		if (options.opcodeSimplifyDescriptors && s.contains("/")) {
			s = s.substring(s.lastIndexOf("/") + 1);
			if (s.endsWith(";")) {
				s = s.substring(0, s.length() - 1);
			}
		}

		return s;
	}

	/**
	 * Italicize the given text.
	 * 
	 * @param input
	 * @return
	 */
	private static String italic(String input) {
		return "<i>" + input + "</i>";
	}

	/**
	 * Color the given text.
	 * 
	 * @param color
	 * @param input
	 * @return
	 */
	private static String color(String color, String input) {
		return "<span style=\"color:" + color + ";\">" + input + "</span>";
	}
}
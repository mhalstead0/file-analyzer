package com.matthalstead.fileanalyzer.consumerfactory;

import java.util.List;

public class ConsumerDefinition {

	private String className;
	private String panelClassName;
	private List<ConstructorArgument> arguments;
	private boolean disabled;
	private boolean root;

	public List<ConstructorArgument> getArguments() {
		return arguments;
	}

	public void setArguments(List<ConstructorArgument> arguments) {
		this.arguments = arguments;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public boolean isRoot() {
		return root;
	}
	
	public void setRoot(boolean root) {
		this.root = root;
	}
	
	public String getPanelClassName() {
		return panelClassName;
	}
	
	public void setPanelClassName(String panelClassName) {
		this.panelClassName = panelClassName;
	}

	public static class ConstructorArgument {
		private String name;
		private String type;
		private String value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

}

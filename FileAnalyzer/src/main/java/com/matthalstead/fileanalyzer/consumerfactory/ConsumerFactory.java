package com.matthalstead.fileanalyzer.consumerfactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.matthalstead.fileanalyzer.InputStreamConsumer;
import com.matthalstead.fileanalyzer.consumerfactory.ConsumerDefinition.ConstructorArgument;
import com.matthalstead.fileanalyzer.util.XMLUtils;

public class ConsumerFactory {

	public static List<ConsumerDefinition> loadConsumerDefinitions() {
		try {
			InputStream is = ConsumerFactory.class.getClassLoader().getResourceAsStream("com/matthalstead/fileanalyzer/consumerfactory/consumers.xml");
			final List<ConsumerDefinition> result = new ArrayList<ConsumerDefinition>();
			XMLUtils.simpleParse(is, new XMLUtils.DefaultSimpleParseReceiver() {
				String basePackage = null;
				ConsumerDefinition currentCD = null;
				ConsumerDefinition.ConstructorArgument currentArg = null;
				@Override
				public void startElement(String name, List<String> ancestorNames, Map<String, String> attributes) {
					if ("consumers".equals(name)) {
						basePackage = attributes.get("basePackage");
					} else if ("consumer".equals(name)) {
						currentCD = new ConsumerDefinition();
					} else if ("constructorArgument".equals(name)) {
						currentArg = new ConsumerDefinition.ConstructorArgument();
					}
				}
				public void simpleElement(String name, List<String> ancestorNames, String content) {
					if (currentCD != null) {
						if (listEndsWith(ancestorNames, "consumers", "consumer")) {
							if ("class".equals(name)) {
								currentCD.setClassName((basePackage == null) ? content : basePackage + content);
							} else if ("panelClass".equals(name)) {
								currentCD.setPanelClassName((basePackage == null) ? content : basePackage + content);
							} else if ("disabled".equals(name)) {
								currentCD.setDisabled("true".equalsIgnoreCase(content.trim().toLowerCase()));
							} else if ("root".equals(name)) {
								currentCD.setRoot("true".equalsIgnoreCase(content.trim().toLowerCase()));
							}
						} else if (listEndsWith(ancestorNames, "constructorArguments", "constructorArgument")) {
							if (currentArg != null) {
								if ("name".equals(name)) {
									currentArg.setName(content);
								} else if("type".equals(name)) {
									currentArg.setType(content);
								} else if ("value".equals(name)) {
									currentArg.setValue(content);
								}  
							}
						}
					}
					
				}
				
				@Override
				public void endElement(String name, List<String> ancestorNames) {
					if ("consumer".equals(name)) {
						if (currentCD != null) {
							result.add(currentCD);
							currentCD = null;
						}
					} else if ("constructorArgument".equals(name)) {
						if (currentCD != null && currentArg != null) {
							List<ConstructorArgument> args = currentCD.getArguments();
							if (args == null) {
								args = new ArrayList<ConstructorArgument>();
								currentCD.setArguments(args);
							}
							args.add(currentArg);
						}
						currentArg = null;
							
					}
				}
			});
			return result;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static InputStreamConsumer buildConsumer(ConsumerDefinition def) {
		try {
			Class<?> c = Class.forName(def.getClassName());
			List<ConstructorArgument> args = def.getArguments();
			InputStreamConsumer isc;
			if (args == null || args.isEmpty()) {
				isc = (InputStreamConsumer) c.newInstance();
			} else {
				Class<?>[] argTypes = new Class<?>[args.size()];
				Object[] argValues = new Object[args.size()];
				for (int i=0; i<argTypes.length; i++) {
					ConstructorArgument arg = args.get(i);
					ClassDef cd = classDefMap.get(arg.getType());
					argTypes[i] = cd.getC();
					argValues[i] = cd.getValue(arg.getValue());
				}
				Constructor<?> constructor = c.getConstructor(argTypes);
				isc = (InputStreamConsumer) constructor.newInstance(argValues);
			}
			return isc;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
	
	private static final ClassDef BOOLEAN_CD = new ClassDef(boolean.class) {
		@Override
		protected Object parse(String str) { return "true".equalsIgnoreCase(str.trim()); }
	};
	private static final ClassDef SHORT_CD = new ClassDef(short.class) {
		@Override
		protected Object parse(String str) { return Short.valueOf(str); };
	};
	private static final ClassDef INT_CD = new ClassDef(int.class) {
		@Override
		protected Object parse(String str) { return Integer.valueOf(str); };
	};
	private static final ClassDef LONG_CD = new ClassDef(long.class) {
		@Override
		protected Object parse(String str) { return Long.valueOf(str); };
	};
	private static final ClassDef FLOAT_CD = new ClassDef(float.class) {
		@Override
		protected Object parse(String str) { return Float.valueOf(str); };
	};
	private static final ClassDef DOUBLE_CD = new ClassDef(double.class) {
		@Override
		protected Object parse(String str) { return Double.valueOf(str); };
	};
	private static final ClassDef BOOLEAN_OBJ_CD = new ClassDef(Boolean.class, BOOLEAN_CD);
	private static final ClassDef SHORT_OBJ_CD = new ClassDef(Short.class, SHORT_CD);
	private static final ClassDef INT_OBJ_CD = new ClassDef(Integer.class, INT_CD);
	private static final ClassDef LONG_OBJ_CD = new ClassDef(Long.class, LONG_CD);
	private static final ClassDef FLOAT_OBJ_CD = new ClassDef(Float.class, FLOAT_CD);
	private static final ClassDef DOUBLE_OBJ_CD = new ClassDef(Double.class, DOUBLE_CD);
	
	private static final List<ClassDef> CLASS_DEFS = Collections.unmodifiableList(Arrays.asList(new ClassDef[] {
			BOOLEAN_CD,
			SHORT_CD,
			INT_CD,
			LONG_CD,
			FLOAT_CD,
			DOUBLE_CD,
			BOOLEAN_OBJ_CD,
			SHORT_OBJ_CD,
			INT_OBJ_CD,
			LONG_OBJ_CD,
			FLOAT_OBJ_CD,
			DOUBLE_OBJ_CD
	}));
	private static final Map<String, ClassDef> classDefMap;
	static {
		Map<String, ClassDef> map = new HashMap<String, ClassDef>();
		for (ClassDef cd : CLASS_DEFS) {
			map.put(cd.name, cd);
		}
		classDefMap = Collections.unmodifiableMap(map);
	}
	
	private static class ClassDef {
		private final String name;
		private final Class<?> c;
		private final ClassDef equivalent;
		public ClassDef(Class<?> c) {
			this(c.getName(), c, null);
		}
		public ClassDef(Class<?> c, ClassDef equivalent) {
			this(c.getName(), c, equivalent);
		}
		public ClassDef(String name, Class<?> c, ClassDef equivalent) {
			this.name = name;
			this.c = c;
			this.equivalent = equivalent;
		}
		
		public Class<?> getC() {
			return c;
		}
		protected Object getValue(String str) {
			if (equivalent != null) {
				return equivalent.getValue(str);
			}
			if (str == null) {
				return null;
			} else {
				return parse(str);
			}
		}
		protected Object parse(String str) {
			throw new RuntimeException("Not implemented");
		}
	}
	
	
}

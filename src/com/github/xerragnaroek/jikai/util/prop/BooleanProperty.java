
package com.github.xerragnaroek.jikai.util.prop;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author XerRagnaroek
 */
public class BooleanProperty extends Property<Boolean> {

	public BooleanProperty(boolean val) {
		super(val);
	}

	public BooleanProperty() {
		super(false);
	}

	public void onChangeToTrue(Runnable onTrue) {
		onChange((b1, b2) -> {
			if (b2) {
				onTrue.run();
			}
		});
	}

	public void onChangeToFalse(Runnable onTrue) {
		onChange((b1, b2) -> {
			if (!b2) {
				onTrue.run();
			}
		});
	}

	public void setIfTrue(boolean val) {
		if (val) {
			set(true);
		}
	}

	public boolean flip() {
		boolean val = !value;
		set(val);
		return val;
	}

	@JsonCreator
	public static BooleanProperty of(boolean val) {
		return new BooleanProperty(val);
	}

}

package mw.nwra.ewaterpermit.constant;

import java.util.HashSet;
import java.util.Set;

public enum Action {
	CREATE, DELETE, UPDATE, VIEW, ALL;

	Action() {
	}

	public Set<Action> getActionTypes() {
		Set<Action> actionTypes = new HashSet<Action>(Action.values().length);

		for (Action action : Action.values()) {
			actionTypes.add(action);
		}

		return actionTypes;
	}
}

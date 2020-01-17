class Minimal {
	void foo(int x) {
		assert 0 <= x : "Precondition does not hold";
		throw new IllegalStateException();
	}
}

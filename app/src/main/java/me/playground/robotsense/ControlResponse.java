package me.playground.robotsense;

public class ControlResponse {
	public int sonarAngle;
	public int angle;
	public int advance;
	public int turn;
	public int distance;

	public ControlResponse(int sonar_angle, int angle, int adv, int trn, int dist) {
		sonarAngle = sonar_angle;
		angle = angle;
		advance = adv;
		turn = trn;
		distance = dist;
	}

	public ControlResponse() {
		sonarAngle = 0;
		angle = 0;
		advance = 0;
		turn = 0;
		distance = 0;
	}
}

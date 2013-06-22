package edu.cmu.mobile;

public class ExperimentData {
	int mode;
	int seq_num;
	int level;
	int errorno;
	int blocks;
	int score;
	long timeError;	
	int latency;
	int hits;
	long time;
	
	/**
	 * Class constructor
	 * @param l
	 * @param e
	 * @param b
	 * @param s
	 * @param tE
	 * @param lat
	 */
	public ExperimentData (int m, int seq, int l, int lat, int e, int b, int s, int h, long tE, long t) {
		mode = m;
		seq_num = seq;
		level = l;
		latency = lat;
		errorno = e;
		blocks = b;
		score = s;
		hits = h;
		timeError = tE;
		time = t;
	}
	
	@Override
    public String toString() {
        return mode + ", " + seq_num + ", " + level + ", " + latency + ", " + errorno + ", "
        		+ blocks + ", " + score + ", " + hits + ", " + timeError + ", " + time;
    }
}

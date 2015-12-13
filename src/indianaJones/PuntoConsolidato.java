package indianaJones;

import java.util.Date;

public class PuntoConsolidato {
	public double vwapRatio;
	public double vwapRatioFTSE;
	public double guadagno;
	public Date tempo;
	public PuntoConsolidato(double vwapRatio, double vwapRatioFTSE,
			double guadagno, Date tempo) {
		super();
		this.vwapRatio = vwapRatio;
		this.vwapRatioFTSE = vwapRatioFTSE;
		this.guadagno = guadagno;
		this.tempo = tempo;
	}
	@Override
	public String toString() {
		return "PuntoConsolidato [tempo=" + tempo + ", vwapRatio=" + vwapRatio
				+ ", vwapRatioFTSE=" + vwapRatioFTSE + ", guadagno=" + guadagno
				+ "]";
	}
	
	
	
}

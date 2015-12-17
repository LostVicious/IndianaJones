package indianaJones;

import java.sql.ResultSet;
import java.util.Date;

public class PuntoConsolidato {
	public String codalfa;
	public double vwapRatio;
	public double vwapRatioFTSE;
	public double guadagno;
	public Date tempo;
	public PuntoConsolidato(String codalfa, double vwapRatio, double vwapRatioFTSE,
			double guadagno, Date tempo) {
		super();
		this.codalfa = codalfa;
		this.vwapRatio = vwapRatio;
		this.vwapRatioFTSE = vwapRatioFTSE;
		this.guadagno = guadagno;
		this.tempo = tempo;
	}
	@Override
	public String toString() {
		return "PuntoConsolidato [codalfa=" + codalfa + ", vwapRatio="
				+ vwapRatio + ", vwapRatioFTSE=" + vwapRatioFTSE
				+ ", guadagno=" + guadagno + ", tempo=" + tempo + "]";
	}
	
	public PuntoConsolidato(ResultSet rs) {
		try {
			this.codalfa = rs.getString("codalfa");
			this.tempo = rs.getTimestamp("tempo");
			this.vwapRatio = rs.getFloat("vwapRatio");
			this.vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
			this.guadagno = rs.getFloat("gainLong25_22");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}

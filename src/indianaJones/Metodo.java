package indianaJones;


public interface Metodo {
	public boolean call(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse);
	public Object getLastResult();
}

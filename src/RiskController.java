import java.util.*;
public class RiskController {
    private List<RiskView> riskViews;

    public RiskController() {
        riskViews = new ArrayList<>();
    }

    public void addRiskView(RiskView rV) {
        riskViews.add(rV);
    }

    public void update() {
        for (RiskView rV: riskViews) {
            rV.notifyViewer();
        }
    }

    public List<RiskView> getRiskViews() {
        return new ArrayList<RiskView>(riskViews);
    }
}

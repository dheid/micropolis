package micropolisj.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CityBudgetTest {

  @Test
  public void testTotalFunds() {
    CityBudget budget = new CityBudget();
    budget.setTotalFunds(1000);
    assertThat(budget.getTotalFunds()).isEqualTo(1000);
  }

  @Test
  public void testTaxFund() {
    CityBudget budget = new CityBudget();
    budget.setTaxFund(500);
    assertThat(budget.getTaxFund()).isEqualTo(500);
  }
}

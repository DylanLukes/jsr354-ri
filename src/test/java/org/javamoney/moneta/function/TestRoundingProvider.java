package org.javamoney.moneta.function;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAdjuster;
import javax.money.MonetaryAmount;

import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.RoundingProviderSpi;

public class TestRoundingProvider implements RoundingProviderSpi {

	private Set<String> customIds = new HashSet<>();

	public TestRoundingProvider() {
		customIds.add("zero");
		customIds.add("minusOne");
		customIds.add("CHF-cash");
	}

	private MonetaryAdjuster zeroRounding = new MonetaryAdjuster() {

		@Override
		public MonetaryAmount adjustInto(MonetaryAmount amount) {
			return Money.ofZero(amount.getCurrency());
		}

	};

	private MonetaryAdjuster minusOneRounding = new MonetaryAdjuster() {

		@Override
		public MonetaryAmount adjustInto(MonetaryAmount amount) {
			return Money.of(amount.getCurrency(), -1);
		}

	};

	private MonetaryAdjuster chfCashRounding = new MonetaryAdjuster() {

		private MonetaryAdjuster minorRounding = MonetaryRoundings.getRounding(
				2, RoundingMode.HALF_UP);

		@Override
		public MonetaryAmount adjustInto(MonetaryAmount amount) {
			MonetaryAmount amt = amount.with(minorRounding);
			Money mp = Money.from(amt.with(MonetaryFunctions.minorPart()));
			BigDecimal delta = null;
			if (mp.isGreaterThanOrEqualTo(Money.of(amount.getCurrency(), 0.03))) {
				// add
				return Money.from(amt).add(
						Money.of(amt.getCurrency(), new BigDecimal("0.05"))
								.subtract(mp));
			}
			else {
				// subtract
				return Money.from(amt).subtract(mp);
			}
		}
	};

	@Override
	public MonetaryAdjuster getRounding(CurrencyUnit currency) {
		if (currency.getCurrencyCode().equals("XXX")) {
			return zeroRounding;
		}
		return null;
	}

	@Override
	public MonetaryAdjuster getRounding(CurrencyUnit currency, long timestamp) {
		if (currency.getCurrencyCode().equals("XXX")) {
			if (timestamp > System.currentTimeMillis()) {
				return minusOneRounding;
			}
			return zeroRounding;
		}
		return null;
	}

	@Override
	public MonetaryAdjuster getCashRounding(CurrencyUnit currency) {
		if (currency.getCurrencyCode().equals("CHF")) {
			return chfCashRounding;
		}
		else if (currency.getCurrencyCode().equals("XXX")) {
			return zeroRounding;
		}
		return null;
	}

	@Override
	public MonetaryAdjuster getCashRounding(CurrencyUnit currency,
			long timestamp) {
		if (currency.getCurrencyCode().equals("CHF")) {
			if (timestamp > System.currentTimeMillis()) {
				return minusOneRounding;
			}
			return chfCashRounding;
		}
		return null;
	}

	@Override
	public MonetaryAdjuster getCustomRounding(String customRoundingId) {
		if ("CHF-cash".equals(customRoundingId)) {
			return chfCashRounding;
		}
		else if ("zero".equals(customRoundingId)) {
			return zeroRounding;
		}
		else if ("minusOne".equals(customRoundingId)) {
			return minusOneRounding;
		}
		return null;
	}

	@Override
	public Set<String> getCustomRoundingIds() {
		return customIds;
	}

}

/*
  Copyright (c) 2012, 2018, Anatole Tresch, Werner Keil and others by the @author tag.

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy of
  the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.
 */
package org.javamoney.moneta;

import org.javamoney.moneta.spi.MonetaryConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.Monetary;
import javax.money.format.AmountFormatContext;
import javax.money.format.AmountFormatContextBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryParseException;

/**
 * Class to format and parse a text string such as 'EUR 25.25' or vice versa using BigDecimal default formatting.
 * This class will used as default by toString and parse in all implementation on Moneta.
 *
 * By default this formatter formats the amounts as {@code AMOUNT.DECIMAL CURRENCY}, e.g. {code 100232.12 CHF}.
 * Hereby the currency is represented by its code and the amount formatted as BigDecimal, rounded on 2 digits after
 * the decimal separator (which always is a dot).
 *
 * You can configure the order of formatting with the {@code org.javamoney.toStringFormatOrder} configuration property:
 *
 * <ul>
 *     <li>Ordering of <b>CURRENCY AMOUNT</b> can be configured with values equal to {@code 'ca', 'c-a', 'c a'}.</li>
 *     <li>Ordering of <b>AMOUNT CURRENCY</b> can be configured with values equal to {@code 'ac', 'a-c', 'a c'} or
 *     any other (default).</li>
 * </ul>
 *
 * Parsing should work either with the currency prefixed or postfixed.
 *
 * <b>Note:</b> This formatter is active by default, but can be replaced with the standard JDK formatter by setting the
 * {@code org.javamoney.moneta.useJDKdefaultFormat} configuration property to {@code true}.
 *
 * {@link Money#toString()}
 * {@link Money#parse(CharSequence)}
 * {@link FastMoney#toString()}
 * {@link FastMoney#parse(CharSequence)}
 * {@link RoundedMoney#toString()}
 * {@link RoundedMoney#parse(CharSequence)}
 * @author Otavio Santana
 * @author Anatole Tresch
 */
public final class ToStringMonetaryAmountFormat implements MonetaryAmountFormat {

    private static final String CONTEXT_PREFIX = "ToString_";

    private static final ToStringMonetaryAmountFormat INSTANCE_FASTMONEY = new ToStringMonetaryAmountFormat(ToStringMonetaryAmountFormatStyle.FAST_MONEY);
    private static final ToStringMonetaryAmountFormat INSTANCE_MONEY = new ToStringMonetaryAmountFormat(ToStringMonetaryAmountFormatStyle.MONEY);
    private static final ToStringMonetaryAmountFormat INSTANCE_ROUNDEDMONEY = new ToStringMonetaryAmountFormat(ToStringMonetaryAmountFormatStyle.ROUNDED_MONEY);

    private final ToStringMonetaryAmountFormatStyle style;

    private final AmountFormatContext context;

    private ToStringMonetaryAmountFormat(ToStringMonetaryAmountFormatStyle style) {
        this.style = Objects.requireNonNull(style);
        context = AmountFormatContextBuilder.of(CONTEXT_PREFIX + style).build();
    }

    public static ToStringMonetaryAmountFormat of(
            ToStringMonetaryAmountFormatStyle style) {
        switch(style){
        case FAST_MONEY:
            return INSTANCE_FASTMONEY;
            case ROUNDED_MONEY:
                return INSTANCE_ROUNDEDMONEY;
            case MONEY:
            default:
                return INSTANCE_MONEY;
        }
    }

    @Override
    public String queryFrom(MonetaryAmount amount) {
		return Optional.ofNullable(amount).map((m) -> {
            BigDecimal dec = amount.getNumber().numberValue(BigDecimal.class);
            dec = dec.setScale(2, RoundingMode.HALF_UP);
            String order = MonetaryConfig.getString("org.javamoney.toStringFormatOrder").orElse("ac");
            switch(order){
                case "currency-amount":
                case "currency amount":
                case "ca":
                case "c a":
                case "c-a":
                    return m.getCurrency().getCurrencyCode() + " " + dec.toPlainString();
                case "amount-currency":
                case "amount currency":
                case "ac":
                case "a c":
                case "a-c":
                default:
                    return dec.toPlainString() + " " + m.getCurrency().getCurrencyCode();
            }
        }).orElse("null");
    }

    @Override
    public AmountFormatContext getContext() {
        return context;
    }

    @Override
    public void print(Appendable appendable, MonetaryAmount amount)
            throws IOException {
        appendable.append(queryFrom(amount));

    }

    @Override
    public MonetaryAmount parse(CharSequence text)
            throws MonetaryParseException {
		try {
			ParserMonetaryAmount amount = parserMonetaryAmount(text);
			return style.to(amount);
		} catch (Exception e) {
			throw new MonetaryParseException(e.getMessage(), text, 0);
		}
    }

    private ParserMonetaryAmount parserMonetaryAmount(CharSequence text) {
        String[] array = Objects.requireNonNull(text).toString().split(" ");
        if(array.length != 2) {
        	throw new MonetaryParseException("An error happened when try to parse the Monetary Amount.",text,0);
        }
        try {
            CurrencyUnit currencyUnit = Monetary.getCurrency(array[1]);
            BigDecimal number = new BigDecimal(array[0]);
            return new ParserMonetaryAmount(currencyUnit, number);
        }catch(Exception e){
            CurrencyUnit currencyUnit = Monetary.getCurrency(array[0]);
            BigDecimal number = new BigDecimal(array[1]);
            return new ParserMonetaryAmount(currencyUnit, number);
        }

    }

    private static class ParserMonetaryAmount {
        ParserMonetaryAmount(CurrencyUnit currencyUnit, BigDecimal number) {
            this.currencyUnit = currencyUnit;
            this.number = number;
        }

        private final CurrencyUnit currencyUnit;
        private final BigDecimal number;
    }

    /**
     * indicates with implementation will used to format or parser in
     * ToStringMonetaryAmountFormat
     */
    public enum ToStringMonetaryAmountFormatStyle {
    	/**
    	 * {@link Money}
    	 */
        MONEY {
            @Override
            MonetaryAmount to(ParserMonetaryAmount amount) {
                return Money.of(amount.number, amount.currencyUnit);
            }
        },
        /**
    	 * {@link FastMoney}
    	 */
        FAST_MONEY {
            @Override
            MonetaryAmount to(ParserMonetaryAmount amount) {
                return FastMoney.of(amount.number, amount.currencyUnit);
            }
        },
        /**
    	 * {@link RoundedMoney}
    	 */
        ROUNDED_MONEY {
            @Override
            MonetaryAmount to(ParserMonetaryAmount amount) {
                return RoundedMoney.of(amount.number, amount.currencyUnit);
            }
        };

        private static final long serialVersionUID = 6606016328162974467L;
        abstract MonetaryAmount to(ParserMonetaryAmount amount);
    }

}

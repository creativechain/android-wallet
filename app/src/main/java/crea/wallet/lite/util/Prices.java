package crea.wallet.lite.util;

import crea.wallet.lite.application.Configuration;
import com.chip_chap.services.cash.Currency;
import com.chip_chap.services.cash.coin.BitCoin;
import com.chip_chap.services.cash.coin.DollarCoin;
import com.chip_chap.services.cash.coin.EurCoin;
import com.chip_chap.services.cash.coin.FairCoin;
import com.chip_chap.services.cash.coin.PesoCoin;
import com.chip_chap.services.cash.coin.ZlotiCoin;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ander on 16/02/16.
 */
public class Prices {

    private static final String TAG = "Prices";

    private static Map<String, Long> euro = new HashMap<>();
    private static Map<String, Long> dollar = new HashMap<>();
    private static Map<String, Long> fair = new HashMap<>();
    private static Map<String, Long> peso = new HashMap<>();
    private static Map<String, Long> zloti = new HashMap<>();
    private static Map<String, Long> bitcoin = new HashMap<>();

    private static final Configuration CONFIG = Configuration.getInstance();
    private static class PriceHolder {
        protected static String BTC = "BTC";
        protected static String USD = "USD";
        protected static String FAC = "FAC";
        protected static String MXN = "MXN";
        protected static String EUR = "EUR";
        protected static String PLN = "PLN";
    }

    public static final class EUR_PRICE extends PriceHolder {

        public static EurCoin getBitCoinPrice() {
            return EurCoin.valueOf(CONFIG.getBtcPrice(Currency.EUR).getLongValue());
        }

        public static EurCoin getDollarPrice() {
            return EurCoin.valueOf(euro.get(USD));
        }

        public EurCoin getFairCoinPrice() {
            return EurCoin.valueOf(euro.get(FAC));
        }

        public static EurCoin getPesoMXPrice() {
            return EurCoin.valueOf(euro.get(MXN));
        }

        public static EurCoin getZlotiPrice() {
            return EurCoin.valueOf(euro.get(PLN));
        }
    }

    public static final class BTC_PRICE extends PriceHolder {

        public static BitCoin getDollarPrice() {
            return BitCoin.valueOf(bitcoin.get(USD));
        }

        public static BitCoin getFairCoinPrice() {
            return BitCoin.valueOf(bitcoin.get(FAC));
        }

        public static BitCoin getPesoMXPrice() {
            return BitCoin.valueOf(bitcoin.get(MXN));
        }

        public static BitCoin getZlotiPrice() {
            return BitCoin.valueOf(bitcoin.get(PLN));
        }

        public static BitCoin getEuroPrice() {
            return BitCoin.valueOf(bitcoin.get(EUR));
        }
    }

    public static final class USD_PRICE extends PriceHolder {

        public static DollarCoin getBitCoinPrice() {
            return DollarCoin.valueOf(CONFIG.getBtcPrice(Currency.USD).getLongValue());
        }

        public static DollarCoin getFairCoinPrice() {
            return DollarCoin.valueOf(dollar.get(FAC));
        }

        public static DollarCoin getPesoMXPrice() {
            return DollarCoin.valueOf(dollar.get(MXN));
        }

        public static DollarCoin getZlotiPrice() {
            return DollarCoin.valueOf(dollar.get(PLN));
        }

        public static DollarCoin getEuroPrice() {
            return DollarCoin.valueOf(dollar.get(EUR));
        }
    }

    public static final class FAC_PRICE extends PriceHolder {

        public static FairCoin getBitCoinPrice() {
            return FairCoin.valueOf(fair.get(BTC));
        }

        public static FairCoin getDollarPrice() {
            return FairCoin.valueOf(fair.get(USD));
        }

        public static FairCoin getPesoMXPrice() {
            return FairCoin.valueOf(fair.get(MXN));
        }

        public static FairCoin getZlotiPrice() {
            return FairCoin.valueOf(fair.get(PLN));
        }

        public static FairCoin getEuroPrice() {
            return FairCoin.valueOf(fair.get(EUR));
        }
    }

    public static final class MXN_PRICE extends PriceHolder {

        public static PesoCoin getBitCoinPrice() {
            return PesoCoin.valueOf(CONFIG.getBtcPrice(Currency.MXN).getLongValue());
        }

        public static PesoCoin getDollarPrice() {
            return PesoCoin.valueOf(peso.get(USD));
        }

        public static PesoCoin getFairCoinPrice() {
            return PesoCoin.valueOf(peso.get(FAC));
        }

        public static PesoCoin getZlotiPrice() {
            return PesoCoin.valueOf(peso.get(PLN));
        }

        public static PesoCoin getEuroPrice() {
            return PesoCoin.valueOf(peso.get(EUR));
        }
    }

    public static final class PLN_PRICE extends PriceHolder {

        public static ZlotiCoin getBitCoinPrice() {
            return ZlotiCoin.valueOf(CONFIG.getBtcPrice(Currency.PLN).getLongValue());
        }

        public static ZlotiCoin getDollarPrice() {
            return ZlotiCoin.valueOf(zloti.get(USD));
        }

        public static ZlotiCoin getFairCoinPrice() {
            return ZlotiCoin.valueOf(zloti.get(FAC));
        }

        public static ZlotiCoin getPesoMXPrice() {
            return ZlotiCoin.valueOf(zloti.get(MXN));
        }

        public static ZlotiCoin getEuroPrice() {
            return ZlotiCoin.valueOf(zloti.get(EUR));
        }
    }

    public static void setEuro(Map<String, Long> euro) {
        Prices.euro = euro;
        CONFIG.setBtcPrice(Currency.EUR, euro.get(PriceHolder.BTC));
    }

    public static void setDollar(Map<String, Long> dollar) {
        Prices.dollar = dollar;
        CONFIG.setBtcPrice(Currency.USD, dollar.get(PriceHolder.BTC));
    }

    public static void setFair(Map<String, Long> fair) {
        Prices.fair = fair;
    }

    public static void setPeso(Map<String, Long> peso) {
        Prices.peso = peso;
        CONFIG.setBtcPrice(Currency.MXN, peso.get(PriceHolder.BTC));
    }

    public static void setZloti(Map<String, Long> zloti) {
        Prices.zloti = zloti;
        CONFIG.setBtcPrice(Currency.PLN, zloti.get(PriceHolder.BTC));
    }

    public static void setBitcoin(Map<String, Long> bitcoin) {
        Prices.bitcoin = bitcoin;
        CONFIG.setBtcPrice(Currency.PLN, bitcoin.get(PriceHolder.PLN));
        CONFIG.setBtcPrice(Currency.EUR, bitcoin.get(PriceHolder.EUR));
        CONFIG.setBtcPrice(Currency.USD, bitcoin.get(PriceHolder.USD));
        CONFIG.setBtcPrice(Currency.MXN, bitcoin.get(PriceHolder.MXN));
    }

}

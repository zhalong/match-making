package com.jrr.dfe;


import java.math.BigDecimal;


/**
 * 限价委托
 * @param <T>
 */
public class LimitPriceCommissionDealMaker<T extends Commission> implements CommissionDealMaker<T>{

    public LimitPriceCommissionDealMaker(){

    }

    @Override
    public void bid(CommissionRecorder<T> cr, CommissionBook<T> bidBook, CommissionBook<T> askBook, DealHandler<T> dealHandler) {
        trade(cr, bidBook, askBook, new BidDealMaker<>(cr), dealHandler);
    }

    @Override
    public void ask(CommissionRecorder<T> cr, CommissionBook<T> bidBook, CommissionBook<T> askBook, DealHandler<T> dealHandler) {
        trade(cr, askBook, bidBook, new AskDealMaker<>(cr), dealHandler);
    }

    private void trade(CommissionRecorder<T> commissionRecorder,
                       CommissionBook<T> own,
                       CommissionBook<T> opponent,
                       DealMaker<T> dealMaker, DealHandler<T> dealHandler) {
        do {
            if(opponent.isEmpty()){
                own.add(commissionRecorder);
                break;
            }
            CommissionRecorder<T> top1 = opponent.head();
            if(!dealMaker.canDeal(top1)){
                own.add(commissionRecorder);
                break;
            }
            Deal<T> deal = dealMaker.makeDeal(top1);
            if(dealHandler != null){
                dealHandler.onDeal(deal);
            }
            if(top1.getCurrentAmount() == 0){
                opponent.remove(top1);
            }
        } while (commissionRecorder.getCurrentAmount() > 0);
    }

    /**
     * 委托成交处理
     */
    private interface DealMaker<T extends Commission> {

        /**
         * 判断是否能够撮合
         * @param opponentMission
         * @return
         */
        boolean canDeal(CommissionRecorder<T> opponentMission);

        /**
         * 撮合
         * @param opponentMission
         * @return
         */
        Deal<T> makeDeal(CommissionRecorder<T> opponentMission);
    }

    private static abstract class AbstractDealMaker<T extends Commission> implements DealMaker<T> {

        protected CommissionRecorder<T> own;

        public AbstractDealMaker(CommissionRecorder<T> own) {
            this.own = own;
        }

        public abstract boolean canDeal(CommissionRecorder<T> opponentMission);

        @Override
        public final Deal<T> makeDeal(CommissionRecorder<T> opponentMission) {
            long dealAmount;
            if(own.getCurrentAmount() < opponentMission.getCurrentAmount()){
                dealAmount = own.getCurrentAmount();
            }else{
                dealAmount = opponentMission.getCurrentAmount();
            }

            BigDecimal dealPrice = opponentMission.getPrice();
            opponentMission.subCurrentAmount(dealAmount);
            own.subCurrentAmount(dealAmount);
            System.out.printf("成交数量%d\n", dealAmount);
            return this.createDeal(dealPrice, dealAmount, opponentMission);
        }

        protected abstract Deal<T> createDeal(BigDecimal dealPrice, long dealAmount, CommissionRecorder<T> opponentMission);
    }

    /**
     * 买成交
     * @param <T>
     */
    private static class BidDealMaker<T extends Commission> extends AbstractDealMaker<T> {

        public BidDealMaker(CommissionRecorder<T> own) {
            super(own);
        }

        @Override
        public boolean canDeal(CommissionRecorder<T> opponentMission) {
            return own.getPrice().compareTo(opponentMission.getPrice()) >= 0;
        }

        @Override
        protected Deal<T> createDeal(BigDecimal dealPrice, long dealAmount, CommissionRecorder<T> opponentMission) {
            SimpleDeal<T> deal = new SimpleDeal<>(dealPrice, dealAmount, own, opponentMission);
            return deal;
        }
    }

    /**
     * 卖成交
     * @param <T>
     */
    private static class AskDealMaker<T extends Commission> extends AbstractDealMaker<T> {

        public AskDealMaker(CommissionRecorder<T> own) {
            super(own);
        }

        @Override
        public boolean canDeal(CommissionRecorder<T> opponentMission) {
            return own.getPrice().compareTo(opponentMission.getPrice()) <= 0;
        }

        @Override
        protected Deal<T> createDeal(BigDecimal dealPrice, long dealAmount, CommissionRecorder<T> opponentMission) {
            SimpleDeal<T> deal = new SimpleDeal<>(dealPrice, dealAmount, opponentMission, own);
            return deal;
        }
    }
}

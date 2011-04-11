#!/bin/sh

CORPUS_NEWS="arff/news*"
CORPUS_BANK="arff/bank*"
RESULTS=10
QUERY_NEWS="comp.graphics/38863 talk.politics.guns/55082 soc.religion.christian/21409 talk.politics.mideast/76075 sci.med/59297 talk.politics.guns/54831 rec.sport.baseball/104988 sci.crypt/15879 misc.forsale/76937 sci.crypt/16074"
QUERY_BANK="A/A0020.txt B/B0414.txt C/C0259.txt D/D0615.txt E/E0853.txt F/F0274.txt"

METRIC1="L1"
METRIC2="L2"

java -Xmx2048M -jar retrieval.jar -i "${CORPUS_NEWS}" -k $RESULTS -m $METRIC1 $QUERY_NEWS > news_q1_k${RESULTS}_${METRIC1}.txt &
java -Xmx2048M -jar retrieval.jar -i "${CORPUS_NEWS}" -k $RESULTS -m $METRIC2 $QUERY_NEWS > news_q1_k${RESULTS}_${METRIC2}.txt
java -Xmx2048M -jar retrieval.jar -i "${CORPUS_BANK}" -k $RESULTS -m $METRIC1 $QUERY_BANK > bank_q1_k${RESULTS}_${METRIC1}.txt &
java -Xmx2048M -jar retrieval.jar -i "${CORPUS_BANK}" -k $RESULTS -m $METRIC2 $QUERY_BANK > bank_q1_k${RESULTS}_${METRIC2}.txt
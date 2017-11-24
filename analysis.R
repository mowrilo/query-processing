library(unbalanced)
library(caret)
library(e1071)
library(ggplot2)

data <- read.csv("/home/murilo/Documentos/rm/project/data/data.csv")
nas <- which(is.na(data$get_out))
data <- data[-nas,]
# data$get_out <- data$get_out*2 - 1

newData <- data[,1:3]
for (col in 4:5){
  factors <- unique(data[,col])
  for (fact in factors){
    newData <- cbind(newData,1*(data[,col] == fact))
    isformer = ""
    if (col==5){isformer="former_"}
    names(newData)[ncol(newData)] <- strcat(isformer,fact)
  }
}
newData <- cbind(newData,data$get_out)
names(newData)[ncol(newData)] <- "get_out"
data <- newData
data$get_out <- as.factor(data$get_out)

ggplot(data,aes(x=get_out,y=jj)) + geom_boxplot()

seps <- c()
for (i in 1:(ncol(data)-1)){
  dC1 <- data[which(data$get_out == 1),i]
  dCm1 <- data[which(data$get_out == 0),i]
  square_means <- (mean(dC1) - mean(dCm1))^2
  var_sums <- var(dC1) + var(dCm1)
  sep <- square_means/var_sums
  seps <- c(seps,sep)
}
gtfo <- which(seps<.001)
data <- data[,-gtfo]

###################################
# SMOTE did not solve the balance problem!

# X <- data[,-ncol(data)]
# Y <- data$get_out

# p <- ubSMOTE(X,Y)
# data <- cbind(p[[1]],p[[2]])
# names(data)[ncol(data)] <- "get_out"

####################################

ici <- sample(nrow(data))
dataTrain <- data[ici[-(1:500)],-ncol(data)]
labTrain <- data[ici[-(1:500)],ncol(data)]
dataTest <- data[ici[1:500],]
labTest <- dataTest$get_out
dataTest <- dataTest[,-ncol(dataTest)]

####################################
# KNN classification

pred.knn <- knn(dataTrain,dataTest,as.factor(labTrain),k=1)
acc <- sum(as.numeric(pred.knn) == as.numeric(labTest))/length(pred.knn)
bad_errors <- sum((as.numeric(pred.knn) == 2) & (as.numeric(labTest) == 1))
ok_errors <- sum((as.numeric(pred.knn) == 1) & (as.numeric(labTest) == 2))
#/
  # sum(as.numeric(pred.knn) != as.numeric(labTest))
cat(sprintf("Accuracy: %.2f",acc*100))
cat(sprintf("Number of 'bad' errors: %.2f",bad_errors))
cat(sprintf("Number of 'ok' errors: %.2f",ok_errors))

######################################################
# Logistic regression
dataTrain <- data[ici[-(1:500)],]
log.model <- glm(get_out~.,data=dataTrain,family=binomial(link='logit'))
log.pred <- predict(log.model,dataTest,type='response')
log.pred <- 1*(log.pred > .5)
acc <- sum(log.pred == labTest)/length(log.pred)
bad_errors <- sum((log.pred == 1) & (labTest == 0))
ok_errors <- sum((log.pred == 0) & (labTest == 1))

cat(sprintf("Accuracy: %.2f",acc*100))
cat(sprintf("Number of 'bad' errors: %.2f",bad_errors))
cat(sprintf("Number of 'ok' errors: %.2f",ok_errors))

#######################################################
# SVM brought complexity, but not accuracy improvement.
# Also, it is more sensitive for the unbalanced data.

metodo <- "svmRadial"

ctrl <- caret::trainControl(method = "repeatedcv",
                     repeats = 1, classProbs = TRUE)
# summaryFunction = twoClassSummary)

tune <- caret::train(get_out ~.,data=dataTrain , method = metodo,
                 verbose=FALSE,trControl = ctrl, metric="ROC")

pred <- predict(tune, dataTest)

acc <- sum(as.numeric(pred) == as.numeric(labTest))/length(pred)
bad_errors <- sum((as.numeric(pred) == 2) & (as.numeric(labTest) == 1))/
  sum(as.numeric(pred) != as.numeric(labTest))

cat(sprintf("Accuracy: %.2f",acc*100))
cat(sprintf("Rate of 'bad' errors: %.2f",100*bad_errors))
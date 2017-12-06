prec_rec_knn <- read.csv("~/Documentos/rm/project/data/precisionKnn.csv")
map_knn <- read.csv("~/Documentos/rm/project/data/mapKnn.csv")
map_knn[,2:3] <- map_knn[,2:3]/50 

prec_rec_log <- read.csv("~/Documentos/rm/project/data/precisionLog.csv")
map_log <- read.csv("~/Documentos/rm/project/data/mapLog.csv")
map_log[,2:3] <- map_log[,2:3]/50 

# For Logistic Regression

seqt <- unique(prec_rec_log$threshold)#seq(.5,.95,.05)
med_prec_mod <- c()
med_prec_orig <- c()
med_rec_mod <- c()
med_rec_orig <- c()
for (i in seqt){
  dat <- prec_rec_log[which(prec_rec_log$threshold == i),]
  med_prec_mod <- c(med_prec_mod,mean(dat$p_at_10_mod))
  med_prec_orig <- c(med_prec_orig,mean(dat$p_at_10_orig))
  med_rec_mod <- c(med_rec_mod,mean(dat$r_at_10_mod))
  med_rec_orig <- c(med_rec_orig,mean(dat$r_at_10_orig))
}
mean_pa10_log <- cbind(seqt,med_prec_orig,med_prec_mod)
mean_ra10_log <- cbind(seqt,med_rec_orig,med_rec_mod)

# For KNN

seqt <- unique(prec_rec_knn$threshold)#seq(.5,.95,.05)
med_prec_mod <- c()
med_prec_orig <- c()
med_rec_mod <- c()
med_rec_orig <- c()
for (i in seqt){
  dat <- prec_rec_knn[which(prec_rec_knn$threshold == i),]
  med_prec_mod <- c(med_prec_mod,mean(dat$p_at_10_mod))
  med_prec_orig <- c(med_prec_orig,mean(dat$p_at_10_orig))
  med_rec_mod <- c(med_rec_mod,mean(dat$r_at_10_mod))
  med_rec_orig <- c(med_rec_orig,mean(dat$r_at_10_orig))
}
mean_pa10_knn <- cbind(seqt,med_prec_orig,med_prec_mod)
mean_ra10_knn <- cbind(seqt,med_rec_orig,med_rec_mod)

ggplot(data=as.data.frame(mean_pa10_log),aes(x=seqt,y=med_prec_mod)) + geom_line(color='blue') +
  geom_line(aes(x=seqt,y=med_prec_orig),color='red') + ylab("Average P@10") + xlab("Threshold")

ggplot(data=as.data.frame(mean_pa10_knn),aes(x=seqt,y=med_prec_mod)) + geom_line(color='blue') +
  geom_line(aes(x=seqt,y=med_prec_orig),color='red') + ylab("Average P@10") + xlab("K")

ggplot(data=as.data.frame(mean_ra10_log),aes(x=seqt,y=med_rec_mod)) + geom_line(color='blue') +
  geom_line(aes(x=seqt,y=med_rec_orig),color='red') + ylab("Average R@10") + xlab("Threshold")

ggplot(data=as.data.frame(mean_ra10_knn),aes(x=seqt,y=med_rec_mod)) + geom_line(color='blue') +
  geom_line(aes(x=seqt,y=med_rec_orig),color='red') + ylab("Average R@10") + xlab("K")

ggplot(data=as.data.frame(map_log),aes(x=threshold,y=map_mod)) + geom_line(color='blue') +
  geom_line(aes(x=threshold,y=map_orig),color='red') + ylab("Mean Average Precision") + xlab("Threshold")

ggplot(data=as.data.frame(map_knn),aes(x=threshold,y=map_mod)) + geom_line(color='blue') +
  geom_line(aes(x=threshold,y=map_orig),color='red') + ylab("Mean Average Precision") + xlab("K")

timeKnn <- read.csv("~/Documentos/rm/project/data/timeKnn.csv")
medTempoKnn <- mean(timeKnn[[1]])
sdTempoKnn <- sd(timeKnn[[1]])
medTempoKnn/1000000
sdTempoKnn/1000000

timeLog <- read.csv("~/Documentos/rm/project/data/timeLog.csv")
medTempoLog <- mean(timeLog[[1]])
sdTempoLog <- sd(timeLog[[1]])
medTempoLog/1000000
sdTempoLog/1000000

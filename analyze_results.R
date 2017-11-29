prec_rec_knn <- read.csv("~/Documentos/rm/project/data/precisionKnn.csv")
map_knn <- read.csv("~/Documentos/rm/project/data/mapKnn.csv")

prec_rec_log <- read.csv("~/Documentos/rm/project/data/precisionLog.csv")
map_log <- read.csv("~/Documentos/rm/project/data/mapLog.csv")

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
mean_pa10 <- cbind(seqt,med_prec_orig,med_prec_mod)
mean_ra10 <- cbind(seqt,med_rec_orig,med_rec_mod)

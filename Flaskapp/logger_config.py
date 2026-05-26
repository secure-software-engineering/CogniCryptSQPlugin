import logging

def get_aifix_logger(name: str = __name__) -> logging.Logger:
    logger = logging.getLogger(name + ".aifix")
    if not logger.hasHandlers():
        logger.setLevel(logging.INFO)

        # File handler
        fh = logging.FileHandler('aifix.log', mode='a')
        fh.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))

        # Console handler
        # ch = logging.StreamHandler()
        # ch.setFormatter(logging.Formatter('%(levelname)s - %(message)s'))

        logger.addHandler(fh)
        # logger.addHandler(ch)

    return logger

def get_fp_logger(name: str = __name__) -> logging.Logger:
    logger = logging.getLogger(name + ".fp")
    if not logger.hasHandlers():
        logger.setLevel(logging.INFO)

        # File handler
        fh = logging.FileHandler('fp.log', mode='a')
        fh.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))

        # Console handler
        ch = logging.StreamHandler()
        ch.setFormatter(logging.Formatter('%(levelname)s - %(message)s'))

        logger.addHandler(fh)
        logger.addHandler(ch)

    return logger
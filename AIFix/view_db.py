import app_db

def main():
    all_records = app_db.get_all_records()
    for rec in all_records:
        print(rec)

if __name__ == '__main__':
    main()

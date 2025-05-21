# Tugas Kecil 3 Strategi Algoritma IF2211

## Penjelasan Singkat

Rush Hour adalah permainan *puzzle* logika berbasis grid yang menantang pemain untuk menggeser kendaraan di dalam sebuah kotak (biasanya berukuran 6x6) agar mobil utama dapat keluar melalui pintu keluar di sisi papan. Setiap kendaraan hanya bisa bergerak lurus ke depan atau ke belakang sesuai dengan orientasinya (horizontal atau vertikal), dan tidak dapat berputar. Tujuan utamanya adalah memindahkan mobil utama ke pintu keluar dengan jumlah langkah seminimal mungkin.

Program ini memecahkan *puzzle* Rush Hour secara otomatis. Ada tiga algoritma yang dipakai, yaitu:

* *Greedy Best-first Search*
* *Uniform Cost Search*
* A*

Heuristik yang dipakai adalah Manhattan *distance*.

Program ini berbasis GUI.

## Requirement / Keperluan

* Java  
  Dicoba pada openjdk 21.0.4 2024-07-16 LTS (Microsoft Build)

## Cara Pakai

* Masuk ke folder src

    ```bash
    cd src
    ```

* *Compile* file GUI.java

    ```bash
    javac GUI.java
    ```

* Jalankan programnya

    ```bash
    java GUI
    ```

## Author

* Jonathan Levi - 13523132

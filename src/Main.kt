import 'package:flutter/material.dart';
import 'ui/screens/home_screen.dart';

void main() {
    runApp(LockInApp());
}

class LockInApp extends StatelessWidget {
    @override
    Widget build(BuildContext context) {
        return MaterialApp(
            title: 'LockIn',
        theme: ThemeData.dark(),
        home: HomeScreen(),
        );
    }
}
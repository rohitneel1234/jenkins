import unittest

from factorial import fact,div

'''from factorial.py file fact and div function imported to test'''

class TestFactorial(unittest.TestCase):
    
    def test_fact(self):
        """
        The actual test.
        Any method which starts with ``test_`` will considered as a test case.
        """
        res = fact(5)

        self.assertEqual(res, 120)


    def test_div(self):
        
        res = div(5)

        self.assertEqual(res, 2)


if __name__ == '__main__':
    unittest.main()
